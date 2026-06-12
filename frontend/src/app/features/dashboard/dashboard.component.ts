import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

interface Company { id: string; name: string; }
interface Stats { tramites: number; workflows: number; activities: number; }

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatProgressSpinnerModule, MatButtonModule, MatIconModule],
  template: `
    <div class="mx-auto flex w-full max-w-[1400px] flex-col gap-6 px-6 py-8">

      <!-- Welcome banner -->
      <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-violet-600 via-purple-600 to-violet-800 p-6 shadow-lg">
        <div class="absolute -right-10 -top-10 h-48 w-48 rounded-full bg-white/5"></div>
        <div class="absolute -bottom-8 right-20 h-32 w-32 rounded-full bg-white/5"></div>
        <div class="relative">
          @if (loading()) {
            <div class="h-7 w-52 animate-pulse rounded-lg bg-white/20"></div>
            <div class="mt-2 h-4 w-64 animate-pulse rounded bg-white/10"></div>
          } @else {
            <h2 class="text-2xl font-bold text-white">Hola, {{ userName() }}</h2>
            <p class="mt-1 text-[14px] text-violet-200/80">{{ companyName() }} · Bienvenido a FlowOS</p>
          }
        </div>
        <div class="relative mt-5 flex flex-wrap gap-3">
          <a routerLink="/tramites"
             class="flex items-center gap-2 rounded-full bg-white/15 px-4 py-2 text-sm font-medium text-white backdrop-blur transition hover:bg-white/25">
            <mat-icon class="!text-[16px]">add_circle</mat-icon>
            Nueva solicitud
          </a>
          <a routerLink="/activities"
             class="flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 text-sm font-medium text-violet-100 backdrop-blur transition hover:bg-white/20">
            <mat-icon class="!text-[16px]">checklist</mat-icon>
            Ver mis tareas
          </a>
        </div>
      </div>

      <!-- Stats cards -->
      @if (statsLoading()) {
        <div class="grid gap-4 sm:grid-cols-3">
          @for (i of [1,2,3]; track i) {
            <div class="h-[88px] animate-pulse rounded-2xl bg-white shadow-sm ring-1 ring-slate-100"></div>
          }
        </div>
      } @else {
        <div class="grid gap-4 sm:grid-cols-3">

          <a routerLink="/tramites"
             class="group flex items-center gap-4 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100 transition hover:shadow-md hover:ring-violet-200">
            <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-2xl bg-violet-50 transition group-hover:bg-violet-100">
              <mat-icon class="text-violet-600">folder_open</mat-icon>
            </div>
            <div>
              <p class="text-2xl font-bold text-slate-900">{{ stats().tramites }}</p>
              <p class="text-sm text-slate-500">Expedientes</p>
            </div>
            <mat-icon class="ml-auto !text-[18px] text-slate-200 transition group-hover:text-violet-300">arrow_forward</mat-icon>
          </a>

          <a routerLink="/workflows"
             class="group flex items-center gap-4 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100 transition hover:shadow-md hover:ring-sky-200">
            <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-2xl bg-sky-50 transition group-hover:bg-sky-100">
              <mat-icon class="text-sky-600">account_tree</mat-icon>
            </div>
            <div>
              <p class="text-2xl font-bold text-slate-900">{{ stats().workflows }}</p>
              <p class="text-sm text-slate-500">Procesos</p>
            </div>
            <mat-icon class="ml-auto !text-[18px] text-slate-200 transition group-hover:text-sky-300">arrow_forward</mat-icon>
          </a>

          <a routerLink="/activities"
             class="group flex items-center gap-4 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100 transition hover:shadow-md hover:ring-amber-200">
            <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-2xl bg-amber-50 transition group-hover:bg-amber-100">
              <mat-icon class="text-amber-600">checklist</mat-icon>
            </div>
            <div>
              <p class="text-2xl font-bold text-slate-900">{{ stats().activities }}</p>
              <p class="text-sm text-slate-500">Tareas pendientes</p>
            </div>
            <mat-icon class="ml-auto !text-[18px] text-slate-200 transition group-hover:text-amber-300">arrow_forward</mat-icon>
          </a>

        </div>
      }

      <!-- Quick access -->
      <div class="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-100">
        <h3 class="mb-4 text-base font-semibold text-slate-900">Acceso rápido</h3>
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          @for (link of quickLinks; track link.route) {
            <a [routerLink]="link.route"
               class="flex items-center gap-3 rounded-xl border border-slate-100 px-4 py-3 text-sm font-medium text-slate-700 transition hover:border-slate-200 hover:bg-slate-50">
              <mat-icon class="!text-[20px]" [style.color]="link.color">{{ link.icon }}</mat-icon>
              {{ link.label }}
            </a>
          }
        </div>
      </div>

    </div>
  `
})
export class DashboardComponent implements OnInit {
  private api  = inject(ApiService);
  private auth = inject(AuthService);

  loading      = signal(true);
  statsLoading = signal(true);
  companyName  = signal('tu empresa');
  stats        = signal<Stats>({ tramites: 0, workflows: 0, activities: 0 });

  quickLinks = [
    { route: '/tramites',     icon: 'folder_open',  color: '#7c3aed', label: 'Expedientes' },
    { route: '/workflows',    icon: 'account_tree', color: '#0891b2', label: 'Procesos' },
    { route: '/activities',   icon: 'checklist',    color: '#d97706', label: 'Mis Tareas' },
    { route: '/report-nlp',   icon: 'psychology',   color: '#a21caf', label: 'IA Predictiva' },
    { route: '/usuario-pide', icon: 'send',         color: '#db2777', label: 'Solicitudes' },
    { route: '/reports',      icon: 'bar_chart',    color: '#e11d48', label: 'Analíticas' },
  ];

  userName() {
    return this.auth.user()?.name || 'Usuario';
  }

  ngOnInit() {
    const companyId = this.auth.user()?.companyId;
    if (!companyId) {
      this.loading.set(false);
    } else {
      this.api.get<Company[]>('/companies').subscribe({
        next: companies => {
          const company = companies.find(item => item.id === companyId);
          this.companyName.set(company?.name || 'tu empresa');
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    }

    forkJoin([
      this.api.get<any[]>('/tramites'),
      this.api.get<any[]>('/workflows'),
      this.api.get<any[]>('/activities')
    ]).pipe(finalize(() => this.statsLoading.set(false))).subscribe({
      next: ([tramites, workflows, activities]) => {
        this.stats.set({
          tramites: tramites.length,
          workflows: workflows.length,
          activities: activities.length
        });
      },
      error: () => {}
    });
  }
}
