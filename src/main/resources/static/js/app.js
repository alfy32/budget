/*
  Keeps your place in a transaction list.

  Transaction lists load 50 at a time as you scroll. When you open a transaction and then go back
  (the back arrow at the top, or the phone's own back button), the list reloads fresh from the
  server, so this script:

    1. when you tap a transaction, remembers the list's address (with any search), how many
       batches of 50 were loaded, and how far down you had scrolled;
    2. when that list loads again because you went back, loads the same number of batches and
       scrolls to the same spot.

  Opening the list from the tab bar starts at the top as usual.
  Everything is kept in sessionStorage, so it's per browser tab and forgotten when the tab closes.
*/
(function () {
    const SAVED = 'budget:list-position';
    const RESTORE = 'budget:list-restore';

    function readSaved() {
        try {
            return JSON.parse(sessionStorage.getItem(SAVED));
        } catch (e) {
            return null;
        }
    }

    function here() {
        return location.pathname + location.search;
    }

    // The same list, ignoring the page/search order of the URL's parameters.
    function sameList(url) {
        const a = new URL(url, location.origin);
        const b = new URL(here(), location.origin);
        return a.pathname === b.pathname
            && (a.searchParams.get('query') || '') === (b.searchParams.get('query') || '')
            && (a.searchParams.get('search') || '') === (b.searchParams.get('search') || '');
    }

    document.addEventListener('click', function (event) {
        // 1. Tapping a transaction in a list: remember where we are.
        const row = event.target.closest('#list a.row');
        if (row) {
            try {
                sessionStorage.setItem(SAVED, JSON.stringify({
                    url: here(),
                    batches: document.querySelectorAll('#list [data-batch]').length,
                    scrollY: window.scrollY,
                }));
            } catch (e) {
                // Private browsing can block storage; going back just starts at the top then.
            }
            return;
        }

        // The back arrow from a transaction to its list: go to the exact list we came from
        // (it may have had a search typed in) and ask it to restore the position.
        const back = event.target.closest('a.back');
        if (back && back.pathname === '/transactions') {
            const saved = readSaved();
            const target = new URL(back.href);
            const savedUrl = saved && new URL(saved.url, location.origin);
            if (savedUrl && savedUrl.pathname === '/transactions'
                && (savedUrl.searchParams.get('query') || '') === (target.searchParams.get('query') || '')) {
                event.preventDefault();
                try {
                    sessionStorage.setItem(RESTORE, '1');
                } catch (e) {
                }
                location.href = saved.url;
            }
        }
    });

    // 2. On a list page: restore the position if we got here by going back.
    async function restore() {
        if (!document.getElementById('list')) {
            return;
        }

        const navigation = performance.getEntriesByType('navigation')[0];
        const cameBack = (navigation && navigation.type === 'back_forward') || sessionStorage.getItem(RESTORE) === '1';
        sessionStorage.removeItem(RESTORE);

        const saved = readSaved();
        if (!cameBack || !saved || !sameList(saved.url)) {
            return;
        }

        history.scrollRestoration = 'manual';

        // Load the batches that were showing before, one after another.
        let loaded = document.querySelectorAll('#list [data-batch]').length;
        while (loaded < saved.batches) {
            const placeholder = document.querySelector('#list .loading-more');
            if (!placeholder) {
                break;
            }
            await htmx.ajax('GET', placeholder.getAttribute('hx-get'), {target: placeholder, swap: 'outerHTML'});
            loaded = document.querySelectorAll('#list [data-batch]').length;
        }

        window.scrollTo(0, saved.scrollY);
    }

    document.addEventListener('DOMContentLoaded', restore);
})();

/*
  Keeps browser history to "list → transaction" while you edit a transaction.

  Category, tags and split are side trips from a transaction. Links and forms marked data-replace
  (and those pages' back arrows) replace the current history entry instead of adding one, so the
  phone's back button from a transaction always goes to the list, not to the category picker.
*/
(function () {
    function isSideTrip() {
        return /^\/transactions\/[^/]+\/(category|tags)$/.test(location.pathname) || location.pathname.startsWith('/split/');
    }

    document.addEventListener('click', function (event) {
        const link = event.target.closest('a[data-replace]') || (isSideTrip() && event.target.closest('a.back'));
        if (link && !event.defaultPrevented && !event.metaKey && !event.ctrlKey) {
            event.preventDefault();
            location.replace(link.href);
        }
    });

    // Send the form, then replace this page with wherever the server sends us.
    document.addEventListener('submit', async function (event) {
        const form = event.target;
        if (!form.matches('form[data-replace]') || event.defaultPrevented) {
            return;
        }
        event.preventDefault();
        try {
            const response = await fetch(form.action, {method: 'POST', body: new FormData(form, event.submitter)});
            if (!response.ok) {
                throw new Error(response.status);
            }
            location.replace(response.url);
        } catch (e) {
            // Something went wrong: fall back to a normal submit so the error shows.
            HTMLFormElement.prototype.submit.call(form);
        }
    });
})();
