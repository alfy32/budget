import {Component} from '@angular/core';

import {HttpClientModule} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {TransactionService} from "../transaction.service";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, HttpClientModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

  constructor(
    private router: Router,
    private transactionService: TransactionService,
  ) {
  }

  createTransaction(): void {
    this.transactionService.createTransaction().subscribe(transactionId => {
      this.router.navigate(['/transactions/' + transactionId]);
    });
  }
}
