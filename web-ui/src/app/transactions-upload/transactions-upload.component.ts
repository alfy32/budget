import {Component} from '@angular/core';
import {FormsModule} from "@angular/forms";
import {NgForOf} from "@angular/common";

@Component({
  selector: 'app-transactions-upload',
  standalone: true,
  imports: [FormsModule, NgForOf],
  templateUrl: './transactions-upload.component.html',
  styleUrl: './transactions-upload.component.css'
})
export class TransactionsUploadComponent {
  accounts: string[] = [
    "Gunnison Checking",
    "Zions Cash Back Visa",
    "First Choice Platinum",
    "Zions All",
  ];

  account?: String = this.accounts[0];

  onFileSelected(event: Event) {
    let target: any = event.target;
    if (target) {
      let file = target.files[0];
      if (file != null && file.name != null) {
        if (file.name.startsWith('export_')) {
          this.account = "Gunnison Checking";
        } else if (file.name.startsWith('transactions-')) {
          this.account = "Zions Cash Back Visa";
        }
      }
    }
  }

}
