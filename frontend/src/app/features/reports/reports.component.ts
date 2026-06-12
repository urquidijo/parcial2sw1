import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/services/api.service';
import { ReportsRealtimeService } from '../../core/services/reports-realtime.service';

interface RolePerformance { departmentName: string; jobRoleName: string; finishedEarly: number; finishedLate: number; averageDurationMinutes: number; averageAvgMinutes: number; }
interface DepartmentFlow { departmentName: string; total: number; }
interface DashboardStats { totalTramites: number; byStatus: Record<string, number>; rolePerformance: RolePerformance[]; departmentFlow: DepartmentFlow[]; }

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, MatIconModule],
  template: `
    <div class="mx-auto max-w-7xl p-6">

      <!-- Header -->
      <div class="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl font-bold text-slate-900">Analíticas</h2>
          <p class="mt-1 text-sm text-slate-500">Métricas y rendimiento del sistema en tiempo real.</p>
        </div>
        <div class="flex items-center gap-2 rounded-full px-4 py-1.5 text-sm font-medium"
             [class.bg-emerald-50]="realtimeConnected()" [class.text-emerald-700]="realtimeConnected()"
             [class.border]="true" [class.border-emerald-200]="realtimeConnected()"
             [class.bg-slate-100]="!realtimeConnected()" [class.text-slate-500]="!realtimeConnected()"
             [class.border-slate-200]="!realtimeConnected()">
          <div class="h-2 w-2 rounded-full" [class.bg-emerald-500]="realtimeConnected()" [class.bg-slate-400]="!realtimeConnected()"></div>
          {{ realtimeConnected() ? 'Tiempo real activo' : 'Tiempo real desconectado' }}
        </div>
      </div>

      @if (loading()) {
        <div class="flex justify-center py-16"><mat-spinner /></div>
      } @else {

        <!-- Main stat -->
        <div class="mb-6 grid gap-4 sm:grid-cols-3">
          <div class="col-span-1 flex items-center gap-4 rounded-2xl bg-gradient-to-br from-violet-600 to-purple-700 p-5 text-white shadow-sm">
            <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-2xl bg-white/20">
              <mat-icon>folder_open</mat-icon>
            </div>
            <div>
              <p class="text-3xl font-bold">{{ stats()?.totalTramites ?? 0 }}</p>
              <p class="text-sm text-violet-200">Total expedientes</p>
            </div>
          </div>

          @for (item of statusEntries().slice(0,2); track item.key) {
            <div class="flex items-center gap-4 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100">
              <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-2xl" [ngClass]="statusIconBg(item.key)">
                <mat-icon [ngClass]="statusIconColor(item.key)">{{ statusIcon(item.key) }}</mat-icon>
              </div>
              <div>
                <p class="text-2xl font-bold text-slate-900">{{ item.value }}</p>
                <p class="text-sm text-slate-500">{{ statusLabel(item.key) }}</p>
              </div>
            </div>
          }
        </div>

        <div class="grid gap-6 xl:grid-cols-2">

          <!-- By status -->
          <section class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100">
            <h3 class="mb-4 text-base font-semibold text-slate-900">Expedientes por estado</h3>
            <div class="space-y-2.5">
              @for (item of statusEntries(); track item.key) {
                <div class="flex items-center justify-between rounded-xl px-4 py-3 ring-1" [ngClass]="statusRowClass(item.key)">
                  <div class="flex items-center gap-2">
                    <mat-icon class="!text-[16px]" [ngClass]="statusIconColor(item.key)">{{ statusIcon(item.key) }}</mat-icon>
                    <span class="text-sm font-medium text-slate-700">{{ statusLabel(item.key) }}</span>
                  </div>
                  <span class="text-sm font-bold text-slate-900">{{ item.value }}</span>
                </div>
              } @empty {
                <div class="py-6 text-center text-sm text-slate-400">Sin datos</div>
              }
            </div>
          </section>

          <!-- Departments -->
          <section class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100">
            <h3 class="mb-4 text-base font-semibold text-slate-900">Áreas con mayor flujo</h3>
            <div class="space-y-2.5">
              @for (item of topDepartments(); track item.departmentName; let i = $index) {
                <div class="flex items-center gap-3 rounded-xl bg-slate-50 px-4 py-3">
                  <span class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-violet-100 text-[11px] font-bold text-violet-700">{{ i + 1 }}</span>
                  <span class="flex-1 text-sm font-medium text-slate-700">{{ item.departmentName }}</span>
                  <span class="text-sm font-bold text-violet-600">{{ item.total }}</span>
                </div>
              } @empty {
                <div class="py-6 text-center text-sm text-slate-400">Sin datos</div>
              }
            </div>
          </section>

          <!-- Early roles -->
          <section class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100 xl:col-span-2">
            <div class="mb-4 flex items-center gap-2">
              <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50">
                <mat-icon class="!text-[16px] text-emerald-600">schedule</mat-icon>
              </div>
              <h3 class="text-base font-semibold text-slate-900">Completados antes del tiempo estimado</h3>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead class="border-b border-slate-100">
                  <tr class="text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                    <th class="pb-3 pr-4">Área</th><th class="pb-3 pr-4">Cargo</th>
                    <th class="pb-3 pr-4">Cantidad</th><th class="pb-3 pr-4">Promedio (min)</th><th class="pb-3">Estimado (min)</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of tempranoRoles(); track item.departmentName + item.jobRoleName) {
                    <tr class="border-b border-slate-50 hover:bg-slate-50">
                      <td class="py-3 pr-4 text-slate-700">{{ item.departmentName }}</td>
                      <td class="py-3 pr-4 text-slate-700">{{ item.jobRoleName }}</td>
                      <td class="py-3 pr-4">
                        <span class="rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">{{ item.finishedEarly }}</span>
                      </td>
                      <td class="py-3 pr-4 text-slate-600">{{ item.averageDurationMinutes }}</td>
                      <td class="py-3 text-slate-600">{{ item.averageAvgMinutes }}</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5" class="py-8 text-center text-slate-400">Sin datos</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </section>

          <!-- Late roles -->
          <section class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100 xl:col-span-2">
            <div class="mb-4 flex items-center gap-2">
              <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-rose-50">
                <mat-icon class="!text-[16px] text-rose-600">timer_off</mat-icon>
              </div>
              <h3 class="text-base font-semibold text-slate-900">Completados después del tiempo estimado</h3>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead class="border-b border-slate-100">
                  <tr class="text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
                    <th class="pb-3 pr-4">Área</th><th class="pb-3 pr-4">Cargo</th>
                    <th class="pb-3 pr-4">Cantidad</th><th class="pb-3 pr-4">Promedio (min)</th><th class="pb-3">Estimado (min)</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of tardeRoles(); track item.departmentName + item.jobRoleName) {
                    <tr class="border-b border-slate-50 hover:bg-slate-50">
                      <td class="py-3 pr-4 text-slate-700">{{ item.departmentName }}</td>
                      <td class="py-3 pr-4 text-slate-700">{{ item.jobRoleName }}</td>
                      <td class="py-3 pr-4">
                        <span class="rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700">{{ item.finishedLate }}</span>
                      </td>
                      <td class="py-3 pr-4 text-slate-600">{{ item.averageDurationMinutes }}</td>
                      <td class="py-3 text-slate-600">{{ item.averageAvgMinutes }}</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5" class="py-8 text-center text-slate-400">Sin datos</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </section>

        </div>
      }
    </div>
  `
})
export class ReportsComponent implements OnInit, OnDestroy {
  private api      = inject(ApiService);
  private realtime = inject(ReportsRealtimeService);

  stats             = signal<DashboardStats | null>(null);
  loading           = signal(true);
  realtimeConnected = signal(false);

  statusEntries  = computed(() => Object.entries(this.stats()?.byStatus ?? {}).map(([key, value]) => ({ key, value })));
  topDepartments = computed(() => (this.stats()?.departmentFlow ?? []).slice(0, 8));
  tempranoRoles  = computed(() => (this.stats()?.rolePerformance ?? []).filter(i => i.finishedEarly > 0).sort((a, b) => b.finishedEarly - a.finishedEarly).slice(0, 8));
  tardeRoles     = computed(() => (this.stats()?.rolePerformance ?? []).filter(i => i.finishedLate > 0).sort((a, b) => b.finishedLate - a.finishedLate).slice(0, 8));

  statusLabel(s: string) {
    return ({ PENDIENTE: 'Pendiente', EN_PROGRESO: 'En progreso', COMPLETADO: 'Completado', RECHAZADO: 'Rechazado' } as Record<string, string>)[s] ?? s;
  }
  statusIcon(s: string) {
    return ({ PENDIENTE: 'hourglass_empty', EN_PROGRESO: 'sync', COMPLETADO: 'check_circle', RECHAZADO: 'cancel' } as Record<string, string>)[s] ?? 'circle';
  }
  statusIconColor(s: string) {
    return ({ PENDIENTE: 'text-amber-500', EN_PROGRESO: 'text-sky-500', COMPLETADO: 'text-emerald-500', RECHAZADO: 'text-rose-500' } as Record<string, string>)[s] ?? 'text-slate-400';
  }
  statusIconBg(s: string) {
    return ({ PENDIENTE: 'bg-amber-50', EN_PROGRESO: 'bg-sky-50', COMPLETADO: 'bg-emerald-50', RECHAZADO: 'bg-rose-50' } as Record<string, string>)[s] ?? 'bg-slate-50';
  }
  statusRowClass(s: string) {
    return ({ PENDIENTE: 'bg-amber-50/50 ring-amber-100', EN_PROGRESO: 'bg-sky-50/50 ring-sky-100', COMPLETADO: 'bg-emerald-50/50 ring-emerald-100', RECHAZADO: 'bg-rose-50/50 ring-rose-100' } as Record<string, string>)[s] ?? 'bg-slate-50 ring-slate-200';
  }

  ngOnInit() {
    this.api.get<DashboardStats>('/reports/dashboard').subscribe({
      next: stats => { this.stats.set(stats); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
    this.realtime.connect({
      onConnected: () => this.realtimeConnected.set(true),
      onDisconnected: () => this.realtimeConnected.set(false),
      onDashboard: stats => { this.stats.set(stats); this.loading.set(false); this.realtimeConnected.set(true); }
    });
  }

  ngOnDestroy() { this.realtime.disconnect(); }
}
