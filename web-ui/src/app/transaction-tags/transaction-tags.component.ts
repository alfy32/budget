import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
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
  needsCategorized: boolean;
  formArray: FormArray<any>;
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
    private formBuilder: FormBuilder,
    private transactionService: TransactionService
  ) {
    this.transactionId = '';
    this.route.params.subscribe(params => {
      this.transactionId = params['id'];
    });

    this.needsCategorized = false;
    this.route.queryParams.subscribe(params => {
      this.needsCategorized = params['needsCategorized'];
    });

    this.formArray = new FormArray<any>([]);
    this.formGroup = new FormGroup({
      tags: this.formArray
    })
  }

  ngOnInit(): void {
    this.transactionService.getTransaction(this.transactionId).subscribe(transaction => {
      if (transaction.tags) {
        const tagSet = this.getTagSet(transaction.tags);
        console.log("tags:" + tagSet);

        this.tags.forEach(tag => {
          this.formArray.push(new FormControl(tagSet.includes(tag)));
        })
      }
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
          needsCategorized: this.needsCategorized
        }
      });
    });
  }

  getTagSet(tags: string): string[] {
    return tags.split(',').map(tag => tag.trim());
  }

}
