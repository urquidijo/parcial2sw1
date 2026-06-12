import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule
  ],
  template: `
    <div class="flex min-h-screen">

      <!-- Left brand panel (desktop only) -->
      <div class="relative hidden flex-col justify-between overflow-hidden bg-[#0d0d18] p-10 lg:flex lg:w-[44%]">
        <div class="absolute inset-0 bg-gradient-to-br from-violet-900/50 via-purple-950/30 to-transparent"></div>
        <div class="absolute -left-24 -top-24 h-96 w-96 rounded-full bg-violet-600/15 blur-3xl"></div>
        <div class="absolute -bottom-24 right-0 h-72 w-72 rounded-full bg-purple-700/15 blur-3xl"></div>

        <!-- Logo -->
        <div class="relative flex items-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 shadow-lg">
            <mat-icon class="text-white">schema</mat-icon>
          </div>
          <span class="text-xl font-bold text-white">FlowOS</span>
        </div>

        <!-- Hero text -->
        <div class="relative">
          <h2 class="text-[2.3rem] font-bold leading-tight text-white">
            Gestiona tus<br>procesos con<br>
            <span class="bg-gradient-to-r from-violet-400 to-purple-300 bg-clip-text text-transparent">precisión total.</span>
          </h2>
          <p class="mt-5 text-[14px] leading-relaxed text-white/50">
            Automatización de flujos, trazabilidad completa<br>y colaboración en tiempo real.
          </p>
          <div class="mt-8 space-y-3.5">
            @for (feat of features; track feat.icon) {
              <div class="flex items-center gap-3">
                <div class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-violet-500/20">
                  <mat-icon class="!text-[16px] text-violet-400">{{ feat.icon }}</mat-icon>
                </div>
                <span class="text-[13px] text-white/60">{{ feat.label }}</span>
              </div>
            }
          </div>
        </div>

        <div class="relative text-[11px] text-white/25">FlowOS · Sistema de Gestión de Procesos</div>
      </div>

      <!-- Right form panel -->
      <div class="flex flex-1 items-center justify-center bg-gray-50 px-6 py-10">
        <div class="w-full max-w-[380px]">

          <!-- Mobile logo -->
          <div class="mb-8 flex items-center gap-3 lg:hidden">
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-violet-500 to-purple-600">
              <mat-icon class="text-white">schema</mat-icon>
            </div>
            <span class="text-lg font-bold text-slate-900">FlowOS</span>
          </div>

          <h1 class="text-[26px] font-bold text-slate-900">Bienvenido</h1>
          <p class="mt-1.5 text-[14px] text-slate-500">Ingresa tus credenciales para continuar</p>

          <form (ngSubmit)="onSubmit()" class="mt-8">
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Correo electrónico</mat-label>
              <input matInput type="email" [(ngModel)]="email" name="email" required autocomplete="email">
              <mat-icon matPrefix>alternate_email</mat-icon>
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Contraseña</mat-label>
              <input matInput [type]="showPassword ? 'text' : 'password'" [(ngModel)]="password" name="password" required>
              <mat-icon matPrefix>lock_outline</mat-icon>
              <button mat-icon-button matSuffix type="button" (click)="showPassword = !showPassword">
                <mat-icon>{{ showPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            @if (error) {
              <div class="mb-4 flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">
                <mat-icon class="!h-4 !w-4 !text-base flex-shrink-0">error_outline</mat-icon>
                {{ error }}
              </div>
            }

            <button mat-flat-button color="primary" type="submit"
              class="mt-1 h-12 w-full !rounded-2xl text-base font-semibold" [disabled]="loading">
              @if (loading) {
                <mat-spinner diameter="20" />
              } @else {
                Iniciar sesión
              }
            </button>
          </form>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  email = '';
  password = '';
  showPassword = false;
  loading = false;
  error = '';

  features = [
    { icon: 'account_tree', label: 'Editor visual de procesos y flujos' },
    { icon: 'psychology',   label: 'IA Predictiva con TensorFlow integrado' },
    { icon: 'group',        label: 'Colaboración en tiempo real' },
    { icon: 'analytics',    label: 'Analíticas y reportes avanzados' },
  ];

  onSubmit() {
    if (!this.email || !this.password) return;
    this.loading = true;
    this.error = '';
    this.auth.login(this.email, this.password).subscribe({
      next: async () => {
        this.loading = false;
        const navigated = await this.router.navigate(['/dashboard']);
        if (!navigated) {
          this.error = 'No se pudo abrir el dashboard';
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Credenciales inválidas';
      }
    });
  }
}
