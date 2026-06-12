import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

interface Workflow { id: string; name: string; description: string; companyId?: string; companyName?: string; _count: { nodo: number; tramites: number } }
interface Company { id: string; name: string }

@Component({
  selector: 'app-workflow-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, MatCardModule, MatButtonModule, MatIconModule, MatInputModule, MatFormFieldModule, MatProgressSpinnerModule, MatSelectModule, MatSnackBarModule],
  template: `
    <div class="mx-auto flex w-full max-w-[1400px] flex-col gap-6 px-6 py-8">

      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 class="text-2xl font-bold text-slate-900">Procesos</h2>
          <p class="mt-1 text-sm text-slate-500">Diseña y administra los flujos de trabajo de tu organización.</p>
        </div>
        @if (auth.isAdmin()) {
          <button mat-flat-button color="primary" (click)="openForm()" class="!rounded-full">
            <mat-icon>add</mat-icon> Nuevo Proceso
          </button>
        }
      </div>

      @if (loading()) {
        <div class="flex justify-center py-16"><mat-spinner /></div>
      } @else {
        <div class="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
          @for (wf of workflows(); track wf.id) {
            <div class="group flex flex-col overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-100 transition hover:shadow-md hover:ring-violet-200">
              <!-- Top accent bar -->
              <div class="h-1.5 w-full bg-gradient-to-r from-violet-500 to-purple-500"></div>
              <div class="flex flex-1 flex-col p-5">
                <div class="mb-3 flex-1">
                  <div class="mb-1 flex items-start justify-between gap-2">
                    <h3 class="text-base font-semibold text-slate-900">{{ wf.name }}</h3>
                    @if (auth.isAdmin()) {
                      <div class="flex flex-shrink-0 items-center gap-0.5">
                        <button mat-icon-button (click)="openForm(wf)" class="!h-8 !w-8">
                          <mat-icon class="!text-[18px] text-slate-400">edit</mat-icon>
                        </button>
                        <button mat-icon-button (click)="delete(wf.id, wf.name)" class="!h-8 !w-8">
                          <mat-icon class="!text-[18px] text-slate-400 hover:text-rose-500">delete</mat-icon>
                        </button>
                      </div>
                    }
                  </div>
                  <p class="text-sm text-slate-500">{{ wf.description || 'Sin descripción' }}</p>
                  <span class="mt-2 inline-block rounded-full bg-violet-50 px-2.5 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-violet-600">
                    {{ wf.companyName || companyName(wf.companyId) }}
                  </span>
                </div>

                <div class="mb-4 flex gap-4 border-t border-slate-100 pt-3 text-xs text-slate-500">
                  <span class="flex items-center gap-1.5">
                    <mat-icon class="!h-4 !w-4 !text-base text-slate-400">layers</mat-icon>
                    {{ wf._count.nodo }} etapas
                  </span>
                  <span class="flex items-center gap-1.5">
                    <mat-icon class="!h-4 !w-4 !text-base text-slate-400">folder_open</mat-icon>
                    {{ wf._count.tramites }} expedientes
                  </span>
                </div>

                <a [routerLink]="[wf.id, 'editor']"
                   class="flex items-center justify-center gap-2 rounded-xl border border-violet-200 bg-violet-50 px-4 py-2 text-sm font-semibold text-violet-700 transition hover:bg-violet-100">
                  <mat-icon class="!text-[16px]">open_in_new</mat-icon>
                  Abrir Editor
                </a>
              </div>
            </div>
          } @empty {
            <div class="col-span-full flex flex-col items-center justify-center rounded-2xl bg-white px-6 py-20 shadow-sm ring-1 ring-slate-100">
              <div class="flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100">
                <mat-icon class="!text-[32px] text-slate-400">account_tree</mat-icon>
              </div>
              <p class="mt-4 font-medium text-slate-500">No hay procesos registrados.</p>
              @if (auth.isAdmin()) {
                <button mat-flat-button color="primary" (click)="openForm()" class="mt-4 !rounded-full">
                  Crear el primero
                </button>
              }
            </div>
          }
        </div>
      }

      @if (showForm()) {
        <div class="fixed inset-0 z-[1000] flex items-center justify-center bg-slate-950/50 px-4" (click)="showForm.set(false)">
          <div class="w-full max-w-xl overflow-hidden rounded-2xl bg-white shadow-2xl" (click)="$event.stopPropagation()">
            <div class="flex items-center justify-between border-b border-slate-100 px-6 py-4">
              <h3 class="text-lg font-semibold text-slate-900">{{ editId() ? 'Editar proceso' : 'Nuevo Proceso' }}</h3>
              <button mat-icon-button (click)="showForm.set(false)">
                <mat-icon class="text-slate-400">close</mat-icon>
              </button>
            </div>
            <div class="p-6">
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Nombre</mat-label>
                <input matInput [(ngModel)]="formName">
              </mat-form-field>
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Descripción</mat-label>
                <textarea matInput rows="3" [(ngModel)]="formDesc"></textarea>
              </mat-form-field>
              @if (auth.isSuperAdmin()) {
                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Organización</mat-label>
                  <mat-select [(ngModel)]="formCompanyId">
                    @for (c of companies(); track c.id) { <mat-option [value]="c.id">{{ c.name }}</mat-option> }
                  </mat-select>
                </mat-form-field>
              } @else {
                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Organización</mat-label>
                  <input matInput [value]="companyName(formCompanyId)" readonly>
                </mat-form-field>
              }
              <div class="mt-2 flex justify-end gap-2">
                <button mat-button (click)="showForm.set(false)">Cancelar</button>
                <button mat-flat-button color="primary" (click)="save()" class="!rounded-full">Guardar</button>
              </div>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class WorkflowListComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  auth = inject(AuthService);

  workflows = signal<Workflow[]>([]);
  companies = signal<Company[]>([]);
  loading = signal(true);
  showForm = signal(false);
  editId = signal<string | null>(null);
  formName = ''; formDesc = ''; formCompanyId = '';

  ngOnInit() { this.load(); }

  load() {
    this.api.get<Workflow[]>('/workflows').subscribe({ next: w => { this.workflows.set(w); this.loading.set(false); }, error: () => this.loading.set(false) });
    if (this.auth.isAdmin()) this.api.get<Company[]>('/companies').subscribe({ next: c => this.companies.set(c) });
  }

  companyName(id?: string) { return this.companies().find(c => c.id === id)?.name || ''; }

  openForm(wf?: Workflow) {
    this.editId.set(wf?.id ?? null);
    this.formName = wf?.name ?? '';
    this.formDesc = wf?.description ?? '';
    this.formCompanyId = wf?.companyId ?? (this.auth.user()?.companyId ?? '');
    this.showForm.set(true);
  }

  save() {
    if (!this.formName.trim()) { this.snack.open('El nombre es requerido', '', { duration: 2500 }); return; }
    const payload = { name: this.formName.trim(), description: this.formDesc.trim(), companyId: this.formCompanyId };
    const req = this.editId()
      ? this.api.put<Workflow>(`/workflows/${this.editId()}`, payload)
      : this.api.post<Workflow>('/workflows', payload);
    req.subscribe({
      next: wf => {
        this.workflows.update(list => this.editId() ? list.map(w => w.id === wf.id ? wf : w) : [wf, ...list]);
        this.showForm.set(false);
        this.snack.open(this.editId() ? 'Proceso actualizado' : 'Proceso creado', '', { duration: 2500 });
      },
      error: err => this.snack.open(err.error?.message || 'Error al guardar', '', { duration: 3000 })
    });
  }

  delete(id: string, name: string) {
    if (!confirm(`¿Eliminar el proceso "${name}"?`)) return;
    this.api.delete(`/workflows/${id}`).subscribe({
      next: () => { this.workflows.update(list => list.filter(w => w.id !== id)); this.snack.open('Proceso eliminado', '', { duration: 2500 }); },
      error: err => this.snack.open(err.error?.message || 'Error al eliminar', '', { duration: 3000 })
    });
  }
}
