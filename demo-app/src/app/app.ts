import { Component, OnInit, Renderer2, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { LoginStore } from './features/login/login.store';
import { computed } from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class AppComponent implements OnInit {
  private renderer = inject(Renderer2);
  private router = inject(Router);
  private loginStore = inject(LoginStore);

  isDarkMode = false;
  isAuthPage = false;
  isMobileMenuOpen = false;

  readonly isAuthenticated = computed(() => this.loginStore.isAuthenticated());
  readonly isAdmin = computed(() => this.loginStore.role() === 'ADMIN');

  ngOnInit(): void {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
      this.isDarkMode = savedTheme === 'dark';
    } else {
      this.isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    this.applyTheme();

    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: any) => {
      const url: string = e.urlAfterRedirects ?? e.url ?? '';
      this.isAuthPage = url.startsWith('/login') || url.startsWith('/forgot-password');
    });
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  logout(): void {
    this.loginStore.logout();
    this.isMobileMenuOpen = false;
    void this.router.navigate(['/login']);
  }

  toggleTheme(): void {
    this.isDarkMode = !this.isDarkMode;
    localStorage.setItem('theme', this.isDarkMode ? 'dark' : 'light');
    this.applyTheme();
  }

  private applyTheme(): void {
    this.renderer.setAttribute(document.documentElement, 'data-theme', this.isDarkMode ? 'dark' : 'light');
  }
}