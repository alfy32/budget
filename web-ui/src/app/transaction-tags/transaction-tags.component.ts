import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {FormArray, FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {TransactionService} from '../transaction.service';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-transaction-tags',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transaction-tags.component.html',
  styleUrl: './transaction-tags.component.css'
})
export class TransactionTagsComponent implements OnInit {
  transactionId: string;
  query: string = 'all';
  formArray: FormArray;
  formGroup: FormGroup;

  tags: string[] = [
    "Medical",
    "Car",
    "Home Improvement",
    "Savings",
    "Gift",
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService
  ) {
    this.transactionId = '';
    this.route.params.subscribe(params => {
      this.transactionId = params['id'];
    });

    this.route.queryParams.subscribe(params => {
      this.query = params['query'];
    });

    this.formArray = new FormArray<any>([]);
    this.formGroup = new FormGroup({
      tags: this.formArray
    })
  }

  ngOnInit(): void {
    this.transactionService.getTransaction(this.transactionId).subscribe(transaction => {
      let tagSet: string[] = [];
      if (transaction.tags) {
        tagSet = this.getTagSet(transaction.tags);
      }

      this.tags.forEach(tag => {
        this.formArray.push(new FormControl(tagSet.includes(tag)));
      })
    });
  }

  updateTags(): void {
    let selectedTags: string[] = [];
    for (let i = 0; i < this.tags.length; i++) {
      if (this.formArray.value[i]) {
        selectedTags.push(this.tags[i]);
      }
    }
    this.transactionService.setTransactionTags(this.transactionId, selectedTags).subscribe(result => {
      this.router.navigate(['/transactions/' + this.transactionId], {
        queryParams: {
          query: this.query
        }
      });
    });
  }

  getTagSet(tags: string): string[] {
    return tags.split(',').map(tag => tag.trim());
  }

}
