import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/services/auth.service';
import { TfOfflineService } from '../core/services/tf-offline.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, CommonModule,
    MatSidenavModule, MatIconModule, MatButtonModule
  ],
  template: `
    @if (!online()) {
      <div class="fixed inset-x-0 top-0 z-[9999] flex items-center justify-center gap-2 bg-amber-500 px-4 py-2 text-sm font-semibold text-white shadow-lg">
        <mat-icon class="!text-[18px]">wifi_off</mat-icon>
        Sin conexión — mostrando datos en caché. Los modelos IA siguen funcionando.
      </div>
    }

    <mat-sidenav-container class="h-screen bg-gray-50" [class.mt-9]="!online()">
      <mat-sidenav mode="side" opened class="w-[248px]" style="background:#0d0d18;border-right:none;box-shadow:none">
        <div class="flex h-full flex-col">

          <!-- Brand -->
          <div class="mx-3 mt-3 flex items-center gap-3 rounded-2xl bg-gradient-to-br from-violet-600 to-purple-700 px-4 py-[14px]">
            <div class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-xl bg-white/20">
              <mat-icon class="!text-[18px] text-white">schema</mat-icon>
            </div>
            <div class="min-w-0 flex-1">
              <p class="text-[15px] font-bold leading-none text-white">FlowOS</p>
              <p class="mt-0.5 text-[10px] leading-none text-violet-200/70">Gestión de Procesos</p>
            </div>
            @if (!online()) {
              <mat-icon class="!text-[14px] flex-shrink-0 text-amber-300" title="Sin conexión">wifi_off</mat-icon>
            }
          </div>

          <!-- Navigation -->
          <nav class="flex-1 overflow-y-auto px-3 py-4">
            <p class="mb-2 px-2 text-[10px] font-semibold uppercase tracking-widest text-slate-500">General</p>

            <a routerLink="/dashboard" routerLinkActive="!bg-violet-600/20 !text-violet-300"
               class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
              <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-violet-500/20">
                <mat-icon class="!text-[16px]">home</mat-icon>
              </div>
              <span class="text-[13px] font-medium">Inicio</span>
            </a>

            <a routerLink="/workflows" routerLinkActive="!bg-violet-600/20 !text-violet-300"
               class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
              <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-sky-500/20">
                <mat-icon class="!text-[16px]">account_tree</mat-icon>
              </div>
              <span class="text-[13px] font-medium">Procesos</span>
            </a>

            <a routerLink="/tramites" routerLinkActive="!bg-violet-600/20 !text-violet-300"
               class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
              <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-emerald-500/20">
                <mat-icon class="!text-[16px]">folder_open</mat-icon>
              </div>
              <span class="text-[13px] font-medium">Expedientes</span>
            </a>

            <a routerLink="/activities" routerLinkActive="!bg-violet-600/20 !text-violet-300"
               class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
              <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-amber-500/20">
                <mat-icon class="!text-[16px]">checklist</mat-icon>
              </div>
              <span class="text-[13px] font-medium">Mis Tareas</span>
            </a>

            <a routerLink="/report-nlp" routerLinkActive="!bg-violet-600/20 !text-violet-300"
               class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
              <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-fuchsia-500/20">
                <mat-icon class="!text-[16px]">psychology</mat-icon>
              </div>
              <span class="text-[13px] font-medium">IA Predictiva</span>
            </a>

            <a routerLink="/usuario-pide" routerLinkActive="!bg-violet-600/20 !text-violet-300"
               class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
              <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-pink-500/20">
                <mat-icon class="!text-[16px]">send</mat-icon>
              </div>
              <span class="text-[13px] font-medium">Solicitudes</span>
            </a>

            @if (auth.isSuperAdmin()) {
              <div class="my-3 border-t border-white/5"></div>
              <p class="mb-2 px-2 text-[10px] font-semibold uppercase tracking-widest text-slate-500">Sistema</p>
              <a routerLink="/companies" routerLinkActive="!bg-violet-600/20 !text-violet-300"
                 class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
                <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-teal-500/20">
                  <mat-icon class="!text-[16px]">corporate_fare</mat-icon>
                </div>
                <span class="text-[13px] font-medium">Organizaciones</span>
              </a>
            }

            @if (auth.isAdmin()) {
              <div class="my-3 border-t border-white/5"></div>
              <p class="mb-2 px-2 text-[10px] font-semibold uppercase tracking-widest text-slate-500">Administración</p>

              <a routerLink="/departments" routerLinkActive="!bg-violet-600/20 !text-violet-300"
                 class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
                <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-orange-500/20">
                  <mat-icon class="!text-[16px]">business</mat-icon>
                </div>
                <span class="text-[13px] font-medium">Áreas</span>
              </a>

              <a routerLink="/job-roles" routerLinkActive="!bg-violet-600/20 !text-violet-300"
                 class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
                <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-cyan-500/20">
                  <mat-icon class="!text-[16px]">badge</mat-icon>
                </div>
                <span class="text-[13px] font-medium">Cargos</span>
              </a>

              <a routerLink="/users" routerLinkActive="!bg-violet-600/20 !text-violet-300"
                 class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
                <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-lime-500/20">
                  <mat-icon class="!text-[16px]">group</mat-icon>
                </div>
                <span class="text-[13px] font-medium">Equipo</span>
              </a>

              <a routerLink="/reports" routerLinkActive="!bg-violet-600/20 !text-violet-300"
                 class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
                <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-rose-500/20">
                  <mat-icon class="!text-[16px]">bar_chart</mat-icon>
                </div>
                <span class="text-[13px] font-medium">Analíticas</span>
              </a>

              <a routerLink="/document-audit" routerLinkActive="!bg-violet-600/20 !text-violet-300"
                 class="mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-white/50 transition-all hover:bg-white/5 hover:text-white/80">
                <div class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-slate-500/20">
                  <mat-icon class="!text-[16px]">history</mat-icon>
                </div>
                <span class="text-[13px] font-medium">Historial</span>
              </a>
            }
          </nav>

          <!-- IA Status -->
          <div class="border-t border-white/5 px-3 py-2">
            @if (tfReady()) {
              <div class="flex items-center gap-2 rounded-xl bg-emerald-500/10 px-3 py-2 text-xs text-emerald-400">
                <div class="h-1.5 w-1.5 rounded-full bg-emerald-400"></div>
                Modelos IA listos
              </div>
            } @else {
              <div class="flex items-center gap-2 rounded-xl bg-white/5 px-3 py-2 text-xs text-white/30">
                <mat-icon class="!text-[14px]">downloading</mat-icon>
                Cargando modelos IA…
              </div>
            }
          </div>

          <!-- User Profile -->
          <div class="border-t border-white/5 px-3 py-3">
            <div class="flex items-center gap-3">
              <div class="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl text-sm font-bold text-white"
                   [style.background]="userAvatarColor()">
                {{ userInitials() }}
              </div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-[13px] font-semibold text-white">{{ auth.user()?.name || auth.user()?.email }}</p>
                <p class="truncate text-[11px] text-white/40">{{ auth.user()?.jobRoleName || auth.user()?.role }}</p>
              </div>
              <button mat-icon-button (click)="auth.logout()" title="Cerrar sesión">
                <mat-icon class="!text-[18px] text-white/40">logout</mat-icon>
              </button>
            </div>
          </div>

        </div>
      </mat-sidenav>

      <mat-sidenav-content class="overflow-y-auto bg-gray-50">
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `
})
export class ShellComponent implements OnInit, OnDestroy {
  auth    = inject(AuthService);
  tfSvc   = inject(TfOfflineService);
  online  = signal(navigator.onLine);
  tfReady = signal(false);

  private _onOnline  = () => this.online.set(true);
  private _onOffline = () => this.online.set(false);

  ngOnInit(): void {
    window.addEventListener('online',  this._onOnline);
    window.addEventListener('offline', this._onOffline);
    const check = () => {
      if (this.tfSvc.isReady()) { this.tfReady.set(true); return; }
      setTimeout(check, 2000);
    };
    setTimeout(check, 1000);
  }

  ngOnDestroy(): void {
    window.removeEventListener('online',  this._onOnline);
    window.removeEventListener('offline', this._onOffline);
  }

  userInitials(): string {
    const name = this.auth.user()?.name || this.auth.user()?.email || '?';
    return name.split(' ').slice(0, 2).map((n: string) => n[0]).join('').toUpperCase();
  }

  userAvatarColor(): string {
    const colors = ['#7c3aed', '#0891b2', '#059669', '#d97706', '#dc2626', '#0369a1'];
    const name = this.auth.user()?.email || 'user';
    return colors[name.charCodeAt(0) % colors.length];
  }
}
