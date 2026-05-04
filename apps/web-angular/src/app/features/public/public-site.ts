import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, HostListener, OnDestroy, ViewChild, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../../core/api-base';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import { RequestFeedback } from '../shared/components/request-feedback';

type LeadPayload = {
  fullName: string;
  companyName?: string;
  email: string;
  phone?: string;
  city?: string;
  businessType?: string;
  branchCount?: number | null;
  interest?: string;
  message?: string;
};

type NavItem = {
  label: string;
  section: string;
  route: string;
};

type ModuleCard = {
  icon: 'inventory' | 'sales' | 'invoice' | 'vision' | 'reports' | 'branches' | 'shield' | 'mail';
  title: string;
  copy: string;
  color: string;
};

type PlanCard = {
  name: string;
  tagline: string;
  price: string;
  featured?: boolean;
  features: string[];
};

type YoloBox = {
  id: number;
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  confidence: string;
  color: string;
};

@Component({
  selector: 'app-public-site',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, RequestFeedback],
  template: `
    <main class="public-site" id="top" #pageRef (scroll)="onPageScroll($event)">
      <div class="ambient-layer" aria-hidden="true">
        <span class="hero-grid"></span>
        <span class="halo halo-primary"></span>
        <span class="halo halo-accent"></span>
      </div>

      <header class="landing-header" [class.scrolled]="scrolled()">
        <div class="iv-container header-inner">
          <button type="button" class="brand-link" [attr.aria-label]="t('header.home')" (click)="goToSection('top', '/')">
            <img src="/brand/insightvision-logo-dark.svg" alt="InsightVision Enterprise AI" />
          </button>

          <nav class="desktop-nav" [attr.aria-label]="t('header.nav')">
            <button type="button" (click)="goToSection('top', '/')">{{ t('nav.home') }}</button>
            @for (item of navItems(); track item.section) {
              <button type="button" (click)="goToSection(item.section, item.route)">
                {{ item.label }}
              </button>
            }
          </nav>

          <div class="header-actions">
            <button type="button" class="language-pill" (click)="toggleLang()" [attr.aria-label]="t('header.toggleLang')">
              {{ lang() === 'es' ? 'EN' : 'ES' }}
            </button>
            <a routerLink="/login" class="iv-btn iv-btn-secondary iv-btn-sm">{{ t('nav.login') }}</a>
            <button type="button" class="iv-btn iv-btn-primary iv-btn-sm" (click)="goToSection('contact', '/demo')">
              {{ t('nav.demo') }}
            </button>
            <button
              type="button"
              class="menu-button"
              aria-label="Abrir menu"
              [attr.aria-expanded]="drawerOpen()"
              (click)="drawerOpen.update(open => !open)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 7h16M4 12h16M4 17h16" />
              </svg>
            </button>
          </div>
        </div>

        @if (drawerOpen()) {
          <div class="mobile-drawer" role="dialog" aria-label="Menu principal">
            <div class="mobile-panel">
              <div class="mobile-panel-top">
                <img src="/brand/insightvision-logo-dark.svg" alt="InsightVision Enterprise AI" />
                <button type="button" aria-label="Cerrar menu" (click)="drawerOpen.set(false)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 6l12 12M18 6 6 18" />
                  </svg>
                </button>
              </div>
              <button type="button" class="drawer-link" (click)="goToSection('top', '/')">{{ t('nav.home') }}</button>
              @for (item of navItems(); track item.section) {
                <button type="button" class="drawer-link" (click)="goToSection(item.section, item.route)">
                  {{ item.label }}
                </button>
              }
              <button type="button" class="drawer-link" (click)="toggleLang()">
                {{ lang() === 'es' ? 'English (EN)' : 'Español (ES)' }}
              </button>
              <a routerLink="/login" class="iv-btn iv-btn-secondary iv-btn-block" (click)="drawerOpen.set(false)">{{ t('nav.login') }}</a>
              <button type="button" class="iv-btn iv-btn-primary iv-btn-block" (click)="goToSection('contact', '/demo')">
                {{ t('nav.demo') }}
              </button>
            </div>
          </div>
        }
      </header>

      <section class="hero-section" aria-label="InsightVision Enterprise AI">
        <div class="iv-container hero-content">
          <div class="hero-copy iv-fade-up">
            <span class="iv-eyebrow"><i></i> {{ t('hero.eyebrow') }}</span>
            <h1>
              {{ t('hero.titleLead') }}
              <span class="typed-word">
                {{ typedText() }}<i class="iv-caret" aria-hidden="true"></i>
              </span>
            </h1>
            <p>{{ t('hero.subtitle') }}</p>
            <div class="hero-actions">
              <button type="button" class="iv-btn iv-btn-primary iv-btn-lg" (click)="goToSection('contact', '/demo')">
                {{ t('hero.cta.primary') }}
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4 12h15m0 0-5-5m5 5-5 5" />
                </svg>
              </button>
              <button type="button" class="iv-btn iv-btn-secondary iv-btn-lg" (click)="goToSection('features', '/funcionalidades')">
                {{ t('hero.cta.secondary') }}
              </button>
            </div>
            <small class="trust-line iv-mono">{{ t('hero.trust') }}</small>
          </div>

          <div class="dashboard-wrap iv-fade-up" aria-label="Vista previa del dashboard">
            <span class="dashboard-glow" aria-hidden="true"></span>
            <div class="dashboard-mockup">
              <div class="browser-bar">
                <span class="dot red"></span>
                <span class="dot yellow"></span>
                <span class="dot green"></span>
                <div class="address-bar iv-mono">app.insightvisionia.cloud/dashboard</div>
              </div>

              <div class="dashboard-body">
                <aside class="mock-sidebar" aria-hidden="true">
                  <span class="mark">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 3.4 19 7v6.8c0 4.2-3 6.5-7 7.8-4-1.3-7-3.6-7-7.8V7l7-3.6Z" />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v8M8.8 12h6.4" />
                    </svg>
                  </span>
                  @for (icon of sidebarIcons; track icon; let first = $first) {
                    <span class="side-icon" [class.active]="first">
                      @switch (icon) {
                        @case ('dashboard') {
                          <svg viewBox="0 0 24 24"><rect x="4" y="4" width="7" height="7" rx="1.5" /><rect x="13" y="4" width="7" height="7" rx="1.5" /><rect x="4" y="13" width="7" height="7" rx="1.5" /><rect x="13" y="13" width="7" height="7" rx="1.5" /></svg>
                        }
                        @case ('inventory') {
                          <svg viewBox="0 0 24 24"><path d="M4 7.5 12 3l8 4.5-8 4.5L4 7.5Z" /><path d="M4 12l8 4.5L20 12" /></svg>
                        }
                        @case ('sales') {
                          <svg viewBox="0 0 24 24"><path d="M5 19V5" /><path d="M5 19h14" /><path d="M8 15l3-3 2 2 5-6" /></svg>
                        }
                        @case ('invoice') {
                          <svg viewBox="0 0 24 24"><path d="M7 3h10v18l-2-1.5L12 21l-3-1.5L7 21V3Z" /><path d="M9.5 8h5M9.5 12h5M9.5 16h3.5" /></svg>
                        }
                        @case ('vision') {
                          <svg viewBox="0 0 24 24"><path d="M4 8h4l2-2h4l2 2h4v10H4V8Z" /><circle cx="12" cy="13" r="3" /></svg>
                        }
                        @default {
                          <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3" /><path d="M12 3v3M12 18v3M3 12h3M18 12h3" /></svg>
                        }
                      }
                    </span>
                  }
                </aside>

                <div class="mock-content">
                  <div class="mock-head">
                    <div>
                      <strong>{{ t('mock.summary') }}</strong>
                      <small class="iv-mono">{{ liveDateLabel() }}</small>
                    </div>
                    <span class="status-badge success pulse">{{ t('mock.live') }}</span>
                  </div>

                  <div class="kpi-grid">
                    @for (metric of liveMetrics(); track metric.key) {
                      <article class="kpi-card">
                        <small>{{ metric.label }}</small>
                        <div>
                          <strong>
                            {{ metric.prefix }}{{ metric.display }}{{ metric.suffix }}
                          </strong>
                          <span [class.negative]="metric.negative">{{ metric.delta }}</span>
                        </div>
                        <svg viewBox="0 0 84 22" preserveAspectRatio="none" aria-hidden="true">
                          <path [attr.d]="metric.sparkArea" fill="currentColor" opacity="0.16"></path>
                          <path [attr.d]="metric.sparkPath" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path>
                        </svg>
                      </article>
                    }
                  </div>

                  <div class="analytics-grid">
                    <article class="chart-card chart-wide">
                      <div class="chart-head">
                        <strong>{{ t('mock.sales14d') }}</strong>
                        <small class="iv-mono">$ USD</small>
                      </div>
                      <svg class="area-chart" viewBox="0 0 560 180" preserveAspectRatio="none" aria-hidden="true">
                        <defs>
                          <linearGradient id="salesFill" x1="0" x2="0" y1="0" y2="1">
                            <stop offset="0%" stop-color="#6D5DF2" stop-opacity="0.48" />
                            <stop offset="100%" stop-color="#6D5DF2" stop-opacity="0" />
                          </linearGradient>
                          <linearGradient id="eventsFill" x1="0" x2="0" y1="0" y2="1">
                            <stop offset="0%" stop-color="#A78BFA" stop-opacity="0.28" />
                            <stop offset="100%" stop-color="#A78BFA" stop-opacity="0" />
                          </linearGradient>
                        </defs>
                        @for (line of chartGridLines; track line) {
                          <line x1="42" x2="545" [attr.y1]="line" [attr.y2]="line"></line>
                        }
                        <path class="area-fill" [attr.d]="liveAreaSales().area" fill="url(#salesFill)" />
                        <path class="area-line" [attr.d]="liveAreaSales().line" />
                        <path class="area-fill accent" [attr.d]="liveAreaEvents().area" fill="url(#eventsFill)" />
                        <path class="area-line accent" [attr.d]="liveAreaEvents().line" />
                        <text x="20" y="34">{{ liveYAxis()[0] }}</text>
                        <text x="20" y="75">{{ liveYAxis()[1] }}</text>
                        <text x="20" y="116">{{ liveYAxis()[2] }}</text>
                        <text x="20" y="158">{{ liveYAxis()[3] }}</text>
                      </svg>
                    </article>

                    <article class="chart-card">
                      <div class="chart-head">
                        <strong>Vision AI</strong>
                        <small class="iv-mono">{{ visionTickLabel() }}</small>
                      </div>
                      <div class="bar-chart" aria-hidden="true">
                        @for (bar of liveBars(); track $index) {
                          <span [style.height.%]="bar"></span>
                        }
                      </div>
                    </article>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="features" class="iv-section modules-section">
        <div class="iv-container">
          <div class="section-heading">
            <span class="iv-eyebrow"><i></i> {{ t('modules.eyebrow') }}</span>
            <h2>{{ t('modules.title') }}</h2>
            <p>{{ t('modules.subtitle') }}</p>
          </div>

          <div class="module-grid">
            @for (module of modulesList(); track module.title) {
              <article class="module-card">
                <span class="module-icon" [style.--module-color]="module.color">
                  @switch (module.icon) {
                    @case ('inventory') {
                      <svg viewBox="0 0 24 24"><path d="M4 7.5 12 3l8 4.5-8 4.5L4 7.5Z" /><path d="M4 12l8 4.5L20 12" /><path d="M4 16.5 12 21l8-4.5" /></svg>
                    }
                    @case ('sales') {
                      <svg viewBox="0 0 24 24"><path d="M5 19V5" /><path d="M5 19h14" /><path d="M8 15l3-3 2 2 5-6" /></svg>
                    }
                    @case ('invoice') {
                      <svg viewBox="0 0 24 24"><path d="M7 3h10v18l-2-1.5L12 21l-3-1.5L7 21V3Z" /><path d="M9.5 8h5M9.5 12h5M9.5 16h3.5" /></svg>
                    }
                    @case ('vision') {
                      <svg viewBox="0 0 24 24"><path d="M4 8h4l2-2h4l2 2h4v10H4V8Z" /><circle cx="12" cy="13" r="3" /></svg>
                    }
                    @case ('reports') {
                      <svg viewBox="0 0 24 24"><path d="M5 19V5" /><path d="M8 17v-5M12 17V8M16 17v-7M19 19H5" /></svg>
                    }
                    @case ('branches') {
                      <svg viewBox="0 0 24 24"><path d="M4 20h16" /><path d="M6 20V8l6-4 6 4v12" /><path d="M9 20v-6h6v6" /></svg>
                    }
                    @case ('shield') {
                      <svg viewBox="0 0 24 24"><path d="M12 3l7 4v5c0 5-3.5 8-7 9-3.5-1-7-4-7-9V7l7-4Z" /><path d="m9.5 12 1.7 1.7 3.3-3.4" /></svg>
                    }
                    @default {
                      <svg viewBox="0 0 24 24"><path d="M4 6h16v12H4z" /><path d="m4 7 8 6 8-6" /></svg>
                    }
                  }
                </span>
                <h3>{{ module.title }}</h3>
                <p>{{ module.copy }}</p>
              </article>
            }
          </div>
        </div>
      </section>

      <section id="vision" class="iv-section vision-section">
        <span class="section-glow glow-left" aria-hidden="true"></span>
        <div class="iv-container split-grid">
          <div class="section-copy">
            <span class="iv-eyebrow"><i></i> {{ t('vision.eyebrow') }}</span>
            <h2>{{ t('vision.title') }}</h2>
            <p>{{ t('vision.subtitle') }}</p>
            <div class="feature-stack">
              @for (feature of visionFeaturesList(); track feature.title) {
                <article>
                  <span>
                    @switch (feature.icon) {
                      @case ('camera') {
                        <svg viewBox="0 0 24 24"><path d="M4 8h4l2-2h4l2 2h4v10H4V8Z" /><circle cx="12" cy="13" r="3" /></svg>
                      }
                      @case ('cpu') {
                        <svg viewBox="0 0 24 24"><rect x="6" y="6" width="12" height="12" rx="2" /><path d="M9 2v3M15 2v3M9 19v3M15 19v3M2 9h3M2 15h3M19 9h3M19 15h3" /></svg>
                      }
                      @default {
                        <svg viewBox="0 0 24 24"><path d="M13 2 5 13h6l-1 9 8-12h-6l1-8Z" /></svg>
                      }
                    }
                  </span>
                  <div>
                    <strong>{{ feature.title }}</strong>
                    <small>{{ feature.copy }}</small>
                  </div>
                </article>
              }
            </div>
          </div>

          <div class="live-feed">
            <div class="feed-header">
              <span class="live-pill"><i></i> LIVE</span>
              <strong class="iv-mono">CAM-04 · {{ t('vision.branch') }}</strong>
              <small class="iv-mono">1080p · 30fps · YOLOv8</small>
            </div>
            <div class="feed-frame">
              <span class="feed-grid" aria-hidden="true"></span>
              <span class="feed-scan" aria-hidden="true"></span>
              <span class="rec-label iv-mono">REC {{ recClock() }}</span>
              @for (box of visionBoxes(); track box.id) {
                <span
                  class="yolo-box"
                  [style.left.%]="box.x"
                  [style.top.%]="box.y"
                  [style.width.%]="box.width"
                  [style.height.%]="box.height"
                  [style.border-color]="box.color"
                  [style.box-shadow]="'0 0 28px ' + box.color + '55'"
                >
                  <small [style.background]="box.color">{{ box.label }} {{ box.confidence }}</small>
                </span>
              }
              <span class="feed-summary iv-mono">{{ visionBoxes().length }} det · {{ personCount() }} person</span>
            </div>
            <div class="feed-metrics">
              <article>
                <small>{{ t('vision.metric.detections') }}</small>
                <strong>{{ liveDetections() }}</strong>
              </article>
              <article>
                <small>{{ t('vision.metric.confidence') }}</small>
                <strong>{{ liveConfidence() }}%</strong>
              </article>
              <article>
                <small>{{ t('vision.metric.eventsToday') }}</small>
                <strong>{{ liveEventsToday() }}</strong>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section id="invoicing" class="iv-section invoicing-section">
        <div class="iv-container split-grid invoice-grid">
          <div class="invoice-card">
            <div class="invoice-top">
              <div>
                <small class="iv-mono">{{ t('invoice.label') }} · 001-001</small>
                <strong>0000004287</strong>
              </div>
              <span class="status-badge success pulse">{{ t('invoice.authorized') }}</span>
            </div>

            <div class="invoice-meta">
              <div>
                <small>{{ t('invoice.client') }}</small>
                <strong>Constructora Andina S.A.</strong>
                <span>RUC 1791234567001</span>
              </div>
              <div>
                <small>{{ t('invoice.date') }}</small>
                <strong>{{ t('invoice.dateValue') }}</strong>
                <span class="iv-mono">14:32 · {{ t('city') }}</span>
              </div>
            </div>

            <div class="invoice-lines">
              @for (line of invoiceLinesList(); track line.name) {
                <div>
                  <span>{{ line.qty }}× {{ line.name }}</span>
                  <strong>{{ line.total }}</strong>
                </div>
              }
            </div>

            <div class="invoice-total">
              <span>{{ t('invoice.totalIva') }}</span>
              <strong class="iv-mono">$4,397.40</strong>
            </div>

            <div class="resend-row">
              <span>
                <svg viewBox="0 0 24 24"><path d="M4 6h16v12H4z" /><path d="m4 7 8 6 8-6" /></svg>
              </span>
              <div>
                <strong>{{ t('invoice.sentByResend') }}</strong>
                <small class="iv-mono">contacto&#64;constructoraandina.ec · {{ t('invoice.delivered') }} 14:32:08</small>
              </div>
              <b>Resend</b>
            </div>
          </div>

          <div class="section-copy">
            <span class="iv-eyebrow"><i></i> {{ t('inv.eyebrow') }}</span>
            <h2>{{ t('inv.title') }}</h2>
            <p>{{ t('inv.subtitle') }}</p>
            <div class="check-list">
              @for (item of invoiceChecksList(); track item) {
                <span>
                  <svg viewBox="0 0 24 24"><path d="m5 12 4 4L19 6" /></svg>
                  {{ item }}
                </span>
              }
            </div>
          </div>
        </div>
      </section>

      <section id="pricing" class="iv-section pricing-section">
        <div class="iv-container">
          <div class="section-heading">
            <span class="iv-eyebrow"><i></i> {{ t('price.eyebrow') }}</span>
            <h2>{{ t('price.title') }}</h2>
            <p>{{ t('price.subtitle') }}</p>
          </div>

          <div class="pricing-grid">
            @for (plan of plansList(); track plan.name) {
              <article class="plan-card" [class.featured]="plan.featured">
                @if (plan.featured) {
                  <span class="popular">{{ t('price.popular') }}</span>
                }
                <h3>{{ plan.name }}</h3>
                <p>{{ plan.tagline }}</p>
                <strong class="plan-price">{{ plan.price }}</strong>
                <button type="button" class="iv-btn" [class.iv-btn-primary]="plan.featured" [class.iv-btn-secondary]="!plan.featured" (click)="goToSection('contact', '/demo')">
                  {{ t('price.cta') }}
                </button>
                <ul>
                  @for (feature of plan.features; track feature) {
                    <li>
                      <svg viewBox="0 0 24 24"><path d="m5 12 4 4L19 6" /></svg>
                      {{ feature }}
                    </li>
                  }
                </ul>
              </article>
            }
          </div>
        </div>
      </section>

      <section id="contact" class="iv-section contact-section">
        <span class="section-glow glow-right" aria-hidden="true"></span>
        <div class="iv-container contact-grid">
          <div class="contact-info">
            <span class="iv-eyebrow"><i></i> {{ t('contact.eyebrow') }}</span>
            <h2>{{ t('contact.title') }}</h2>
            <p>{{ t('contact.subtitle') }}</p>
            <div class="contact-lines">
              <a class="contact-line" href="mailto:soporte&#64;insightvisionia.cloud">
                <span class="ci">
                  <svg viewBox="0 0 24 24"><path d="M4 6h16v12H4z" /><path d="m4 7 8 6 8-6" /></svg>
                </span>
                <span class="ct">
                  <small>{{ t('contact.email') }}</small>
                  <strong>soporte&#64;insightvisionia.cloud</strong>
                </span>
              </a>
              <a class="contact-line" href="tel:+593995303642">
                <span class="ci">
                  <svg viewBox="0 0 24 24"><path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.7 19.7 0 0 1-8.6-3.1 19.4 19.4 0 0 1-6-6A19.7 19.7 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.3 1.7.6 2.5a2 2 0 0 1-.5 2.1L8 9.5a16 16 0 0 0 6.5 6.5l1.2-1.2a2 2 0 0 1 2.1-.5c.8.3 1.6.5 2.5.6a2 2 0 0 1 1.7 2Z" /></svg>
                </span>
                <span class="ct">
                  <small>{{ t('contact.phone') }}</small>
                  <strong>+593 099 530 3642</strong>
                </span>
              </a>
              <span class="contact-line">
                <span class="ci">
                  <svg viewBox="0 0 24 24"><path d="M12 20s6-5.33 6-10a6 6 0 1 0-12 0c0 4.67 6 10 6 10z" /><circle cx="12" cy="10" r="2.35" /></svg>
                </span>
                <span class="ct">
                  <small>{{ t('contact.location') }}</small>
                  <strong>{{ t('city') }}, Ecuador</strong>
                </span>
              </span>
            </div>

            <div class="map-wrap">
              <iframe
                class="map-iframe"
                title="InsightVision · Cuenca, Ecuador"
                src="https://www.google.com/maps?q=Cuenca,+Ecuador&hl=es&z=13&output=embed"
                loading="lazy"
                referrerpolicy="no-referrer-when-downgrade"
                allowfullscreen
              ></iframe>
              <span class="map-overlay" aria-hidden="true"></span>
              <a
                class="map-cta"
                href="https://www.google.com/maps/place/Cuenca,+Ecuador"
                target="_blank"
                rel="noopener noreferrer"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14 5h5v5M19 5l-7 7M19 14v5H5V5h5" />
                </svg>
                {{ t('contact.openMap') }}
              </a>
            </div>
          </div>

          <form class="lead-form" (ngSubmit)="submitLead()" #leadForm="ngForm" novalidate>
            <header class="lead-form__head">
              <strong>{{ t('form.title') }}</strong>
              <small>{{ t('form.subtitle') }}</small>
            </header>
            @if (feedback(); as notice) {
              <app-request-feedback [tone]="notice.tone" [message]="notice.message" [traceId]="notice.traceId" />
            }

            <fieldset>
              <legend>{{ t('form.section.you') }}</legend>
              <div class="row">
                <label class="wide">
                  <span class="iv-label">{{ t('form.fullName') }}</span>
                  <input class="iv-input" required name="fullName" [(ngModel)]="lead.fullName" maxlength="180" [placeholder]="t('form.fullName.ph')" />
                </label>
                <label>
                  <span class="iv-label">{{ t('form.email') }}</span>
                  <input class="iv-input" required type="email" name="email" [(ngModel)]="lead.email" maxlength="255" placeholder="usuario@empresa.com" />
                </label>
                <label>
                  <span class="iv-label">{{ t('form.phone') }}</span>
                  <input class="iv-input" name="phone" [(ngModel)]="lead.phone" maxlength="50" placeholder="+593 099 530 3642" />
                </label>
              </div>
            </fieldset>

            <fieldset>
              <legend>{{ t('form.section.business') }}</legend>
              <div class="row">
                <label>
                  <span class="iv-label">{{ t('form.company') }}</span>
                  <input class="iv-input" name="companyName" [(ngModel)]="lead.companyName" maxlength="180" [placeholder]="t('form.company.ph')" />
                </label>
                <label>
                  <span class="iv-label">{{ t('form.city') }}</span>
                  <input class="iv-input" name="city" [(ngModel)]="lead.city" maxlength="120" [placeholder]="t('city')" />
                </label>
                <label>
                  <span class="iv-label">{{ t('form.businessType') }}</span>
                  <select class="iv-select" name="businessType" [(ngModel)]="lead.businessType">
                    <option value="">—</option>
                    @for (type of businessTypesList(); track type) {
                      <option [value]="type">{{ type }}</option>
                    }
                  </select>
                </label>
                <label>
                  <span class="iv-label">{{ t('form.branches') }}</span>
                  <select class="iv-select" name="branchCount" [(ngModel)]="lead.branchCount">
                    <option [ngValue]="null">—</option>
                    <option [ngValue]="1">1</option>
                    <option [ngValue]="3">2-5</option>
                    <option [ngValue]="10">6-15</option>
                    <option [ngValue]="16">15+</option>
                  </select>
                </label>
              </div>
            </fieldset>

            <fieldset>
              <legend>{{ t('form.section.interest') }}</legend>
              <div class="row">
                <label class="wide">
                  <span class="iv-label">{{ t('form.interest') }}</span>
                  <select class="iv-select" name="interest" [(ngModel)]="lead.interest">
                    @for (interest of interestsList(); track interest) {
                      <option [value]="interest">{{ interest }}</option>
                    }
                  </select>
                </label>
                <label class="wide">
                  <span class="iv-label">{{ t('form.message') }}</span>
                  <textarea class="iv-textarea" name="message" [(ngModel)]="lead.message" rows="4" maxlength="1200" [placeholder]="t('form.message.ph')"></textarea>
                </label>
              </div>
            </fieldset>

            <div class="form-footer">
              <p>{{ t('form.disclaimer') }}</p>
              <button type="submit" class="iv-btn iv-btn-primary iv-btn-lg" [disabled]="!canSubmit()">
                @if (submitting()) {
                  {{ t('form.sending') }}
                } @else {
                  {{ t('form.submit') }}
                  <svg viewBox="0 0 24 24"><path d="M4 12h15m0 0-5-5m5 5-5 5" /></svg>
                }
              </button>
            </div>
          </form>
        </div>
      </section>

      <footer class="landing-footer">
        <div class="iv-container footer-grid">
          <div class="footer-brand">
            <img src="/brand/insightvision-logo-dark.svg" alt="InsightVision Enterprise AI" />
            <p>{{ t('footer.tagline') }}</p>
            <small class="iv-mono">soporte&#64;insightvisionia.cloud</small>
            <small class="iv-mono">+593 099 530 3642</small>
            <small class="iv-mono">{{ t('city') }}, Ecuador</small>
          </div>
          <div class="footer-cols">
            @for (col of footerColumnsList(); track col.title) {
              <div>
                <strong>{{ col.title }}</strong>
                @for (item of col.items; track item.label) {
                  <button type="button" (click)="goToSection(item.section, item.route)">{{ item.label }}</button>
                }
              </div>
            }
          </div>
        </div>
        <div class="iv-container footer-bottom">
          <span class="iv-mono">© 2026 InsightVision Enterprise AI · {{ t('city') }}, Ecuador</span>
          <span class="iv-mono">Built with Geist · Powered by YOLO · Resend</span>
        </div>
      </footer>
    </main>
  `,
  styles: `
    :host {
      display: block;
      height: 100%;
      overflow: hidden;
      background: #07090e;
    }

    .public-site {
      --iv-bg: #07090e;
      --iv-bg-elevated: #0b1018;
      --iv-surface: #0f151f;
      --iv-surface-2: #121a27;
      --iv-card: #111821;
      --iv-text: #f8fafc;
      --iv-text-muted: #9aa6b7;
      --iv-text-faint: #586274;
      --iv-border: rgba(148, 163, 184, 0.14);
      --iv-border-strong: rgba(148, 163, 184, 0.22);
      --iv-divider: rgba(148, 163, 184, 0.1);
      --iv-primary: #5b4df1;
      --iv-primary-2: #7c3aed;
      --iv-accent: #a78bfa;
      --iv-success: #22c55e;
      --iv-error: #ef4444;
      --iv-warning: #f59e0b;
      --iv-font-display: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      --iv-font-mono: "JetBrains Mono", "SFMono-Regular", Consolas, "Liberation Mono", monospace;
      height: 100dvh;
      overflow-x: hidden;
      overflow-y: auto;
      scroll-behavior: smooth;
      background:
        radial-gradient(circle at 50% -8%, rgba(79, 70, 229, 0.28), transparent 34%),
        radial-gradient(circle at 8% 50%, rgba(124, 58, 237, 0.1), transparent 26%),
        #07090e;
      color: var(--iv-text);
      font-family: var(--iv-font-display);
      letter-spacing: 0;
    }

    .public-site *,
    .public-site *::before,
    .public-site *::after {
      box-sizing: border-box;
    }

    button,
    input,
    select,
    textarea {
      font: inherit;
    }

    button {
      cursor: pointer;
    }

    .iv-container {
      width: min(1240px, calc(100% - 40px));
      margin-inline: auto;
    }

    .iv-mono {
      font-family: var(--iv-font-mono);
    }

    .ambient-layer,
    .hero-grid,
    .halo,
    .section-glow {
      pointer-events: none;
      position: absolute;
    }

    .ambient-layer {
      inset: 0;
      z-index: 0;
      overflow: hidden;
    }

    .hero-grid {
      inset: 0;
      opacity: 0.36;
      background-image:
        linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
        linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
      background-size: 52px 52px;
      mask-image: radial-gradient(ellipse at 50% 30%, black 0%, transparent 72%);
    }

    .halo {
      border-radius: 999px;
      filter: blur(110px);
      opacity: 0.32;
    }

    .halo-primary {
      width: 620px;
      height: 620px;
      top: -260px;
      left: 12%;
      background: radial-gradient(circle, rgba(79, 70, 229, 0.62), transparent 70%);
    }

    .halo-accent {
      width: 520px;
      height: 520px;
      top: -160px;
      right: 8%;
      background: radial-gradient(circle, rgba(139, 92, 246, 0.45), transparent 70%);
    }

    .landing-header {
      position: fixed;
      inset: 0 0 auto;
      z-index: 50;
      height: 78px;
      border-bottom: 1px solid transparent;
      background: transparent;
      transition: background 240ms ease, border-color 240ms ease, backdrop-filter 240ms ease, box-shadow 240ms ease;
    }

    .landing-header.scrolled {
      border-color: rgba(148, 163, 184, 0.12);
      background: rgba(7, 9, 14, 0.72);
      backdrop-filter: blur(24px) saturate(180%);
      -webkit-backdrop-filter: blur(24px) saturate(180%);
      box-shadow: 0 1px 0 rgba(148, 163, 184, 0.04), 0 16px 40px -24px rgba(0, 0, 0, 0.6);
    }

    .header-inner {
      height: 78px;
      display: grid;
      grid-template-columns: clamp(220px, 22vw, 280px) 1fr auto;
      align-items: center;
      gap: 24px;
    }

    .brand-link {
      width: fit-content;
      display: inline-flex;
      align-items: center;
      border: 0;
      padding: 4px 8px 4px 0;
      margin: 0;
      background: transparent;
      filter: drop-shadow(0 6px 18px rgba(139, 92, 246, 0.22));
      transition: filter 200ms ease, transform 200ms ease;
    }
    .brand-link:hover { transform: translateY(-1px); filter: drop-shadow(0 10px 24px rgba(139, 92, 246, 0.34)); }

    .brand-link img {
      width: clamp(180px, 18vw, 230px);
      height: auto;
      display: block;
    }
    .footer-brand img {
      width: clamp(190px, 22vw, 240px);
      height: auto;
      display: block;
    }
    .mobile-panel-top img {
      width: 170px;
      height: auto;
      display: block;
    }

    .desktop-nav {
      display: inline-flex;
      justify-content: center;
      align-items: center;
      gap: 4px;
    }

    .desktop-nav button,
    .footer-cols button {
      border: 0;
      background: transparent;
      color: var(--iv-text-muted);
      text-decoration: none;
      transition: color 150ms ease, background 150ms ease;
    }

    .desktop-nav button {
      padding: 8px 12px;
      border-radius: 7px;
      font-size: 13.5px;
      font-weight: 700;
      white-space: nowrap;
    }

    .desktop-nav button:hover,
    .footer-cols button:hover {
      color: var(--iv-text);
    }

    .header-actions {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .language-pill {
      min-width: 44px;
      height: 32px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0 10px;
      border: 1px solid rgba(148, 163, 184, 0.2);
      border-radius: 999px;
      color: #c4b5fd;
      background: rgba(139, 92, 246, 0.08);
      font-family: var(--iv-font-mono);
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.12em;
      cursor: pointer;
      transition: background 180ms ease, border-color 180ms ease, color 180ms ease, transform 180ms ease;
    }
    .language-pill:hover {
      color: #ddd6fe;
      border-color: rgba(167, 139, 250, 0.5);
      background: rgba(139, 92, 246, 0.16);
      transform: translateY(-1px);
    }

    .iv-btn {
      min-height: 40px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      border-radius: 8px;
      border: 1px solid transparent;
      padding: 0 18px;
      color: var(--iv-text);
      text-decoration: none;
      font-size: 14px;
      font-weight: 800;
      line-height: 1;
      transition: transform 150ms ease, border-color 150ms ease, background 150ms ease, box-shadow 150ms ease;
    }

    .iv-btn:hover {
      transform: translateY(-1px);
    }

    .iv-btn:disabled {
      cursor: not-allowed;
      opacity: 0.58;
      transform: none;
    }

    .iv-btn svg {
      width: 15px;
      height: 15px;
      flex: 0 0 auto;
    }

    .iv-btn-sm {
      min-height: 36px;
      padding-inline: 14px;
      font-size: 13px;
    }

    .iv-btn-lg {
      min-height: 48px;
      padding-inline: 22px;
      font-size: 14px;
      border-radius: 9px;
    }

    .iv-btn-block {
      width: 100%;
    }

    .iv-btn-primary {
      border-color: rgba(167, 139, 250, 0.3);
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      box-shadow: 0 16px 38px rgba(79, 70, 229, 0.32);
      color: #fff;
    }

    .iv-btn-secondary {
      border-color: var(--iv-border-strong);
      background: rgba(15, 23, 42, 0.48);
      color: var(--iv-text);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
    }

    .menu-button {
      width: 38px;
      height: 38px;
      display: none;
      place-items: center;
      border: 1px solid var(--iv-border);
      border-radius: 8px;
      color: var(--iv-text);
      background: rgba(15, 23, 42, 0.65);
    }

    .menu-button svg,
    .mobile-panel-top button svg {
      width: 18px;
      height: 18px;
    }

    .mobile-drawer {
      position: fixed;
      inset: 0;
      z-index: 60;
      display: flex;
      justify-content: flex-end;
      background: rgba(0, 0, 0, 0.5);
      backdrop-filter: blur(10px);
    }

    .mobile-panel {
      width: min(360px, 92vw);
      height: 100%;
      display: grid;
      align-content: start;
      gap: 10px;
      padding: 18px;
      border-left: 1px solid var(--iv-border);
      background: #0b1018;
      box-shadow: -24px 0 70px rgba(0, 0, 0, 0.4);
      animation: drawerIn 220ms ease both;
    }

    .mobile-panel-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .mobile-panel-top button {
      width: 36px;
      height: 36px;
      display: grid;
      place-items: center;
      border: 1px solid var(--iv-border);
      border-radius: 8px;
      color: var(--iv-text);
      background: transparent;
    }

    .drawer-link {
      width: 100%;
      min-height: 42px;
      border: 0;
      border-radius: 9px;
      padding: 0 10px;
      color: var(--iv-text-muted);
      background: transparent;
      text-align: left;
      font-weight: 800;
    }

    .drawer-link:hover {
      color: var(--iv-text);
      background: rgba(255, 255, 255, 0.04);
    }

    .hero-section {
      position: relative;
      z-index: 1;
      min-height: 100vh;
      padding: 168px 0 96px;
      overflow: hidden;
    }
    .hero-section::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -1px;
      height: 220px;
      background: linear-gradient(180deg, transparent 0%, rgba(7, 9, 14, 0.6) 60%, #07090e 100%);
      pointer-events: none;
      z-index: 1;
    }

    .hero-content {
      position: relative;
      z-index: 2;
      text-align: center;
    }

    .hero-copy {
      max-width: 920px;
      margin: 0 auto;
      display: grid;
      justify-items: center;
    }

    .iv-eyebrow {
      width: fit-content;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 7px 12px;
      border: 1px solid rgba(139, 92, 246, 0.38);
      border-radius: 999px;
      color: #c4b5fd;
      background: rgba(139, 92, 246, 0.1);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06), 0 12px 44px rgba(79, 70, 229, 0.12);
      font-family: var(--iv-font-mono);
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.1em;
      text-transform: uppercase;
    }

    .iv-eyebrow i {
      width: 6px;
      height: 6px;
      border-radius: 999px;
      background: #a78bfa;
      box-shadow: 0 0 14px #a78bfa;
    }

    h1,
    h2,
    h3,
    p {
      margin: 0;
    }

    .hero-copy h1 {
      max-width: 920px;
      margin-top: 24px;
      font-size: clamp(42px, 5.7vw, 76px);
      font-weight: 700;
      line-height: 1.04;
      letter-spacing: -0.035em;
      text-wrap: balance;
    }

    .typed-word {
      min-width: 7.2ch;
      display: inline-flex;
      justify-content: flex-start;
      align-items: baseline;
      text-align: left;
      color: transparent;
      background: linear-gradient(120deg, #a78bfa 0%, #8b5cf6 50%, #4f46e5 100%);
      -webkit-background-clip: text;
      background-clip: text;
      white-space: nowrap;
    }

    .iv-caret {
      width: 0.08em;
      height: 0.88em;
      display: inline-block;
      margin-left: 0.05em;
      transform: translateY(0.08em);
      background: #a78bfa;
      box-shadow: 0 0 18px rgba(167, 139, 250, 0.95);
      animation: caretBlink 860ms steps(2, start) infinite;
    }

    .hero-copy p {
      max-width: 660px;
      margin-top: 24px;
      color: var(--iv-text-muted);
      font-size: clamp(15px, 1.4vw, 18px);
      font-weight: 650;
      line-height: 1.58;
      text-wrap: pretty;
    }

    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 12px;
      margin-top: 32px;
    }

    .trust-line {
      margin-top: 24px;
      color: var(--iv-text-faint);
      font-size: 11.5px;
      font-weight: 800;
      letter-spacing: 0.05em;
    }

    .dashboard-wrap {
      position: relative;
      max-width: 1100px;
      margin: 60px auto 0;
      animation-delay: 120ms;
    }

    .dashboard-glow {
      position: absolute;
      inset: -64px;
      border-radius: 40px;
      background:
        radial-gradient(ellipse at 30% 40%, rgba(79, 70, 229, 0.24), transparent 65%),
        radial-gradient(ellipse at 75% 70%, rgba(139, 92, 246, 0.18), transparent 70%);
      filter: blur(50px);
      animation: dashGlow 8s ease-in-out infinite alternate;
    }
    @keyframes dashGlow {
      0%   { opacity: 0.85; transform: scale(1); }
      100% { opacity: 1;    transform: scale(1.04); }
    }

    .dashboard-mockup {
      position: relative;
      overflow: hidden;
      border: 1px solid var(--iv-border-strong);
      border-radius: 16px;
      background: var(--iv-bg-elevated);
      box-shadow: 0 50px 120px -30px rgba(79, 70, 229, 0.45), 0 30px 80px -20px rgba(0, 0, 0, 0.68);
      transform: perspective(1800px) rotateX(2deg);
      transform-style: preserve-3d;
    }

    .browser-bar {
      height: 36px;
      display: flex;
      align-items: center;
      gap: 7px;
      padding: 0 14px;
      border-bottom: 1px solid var(--iv-border);
      background: var(--iv-surface);
    }

    .dot {
      width: 10px;
      height: 10px;
      border-radius: 999px;
    }

    .dot.red { background: #ff5f57; }
    .dot.yellow { background: #febc2e; }
    .dot.green { background: #28c840; }

    .address-bar {
      flex: 1;
      max-width: 360px;
      margin-left: 14px;
      padding: 5px 12px;
      border-radius: 6px;
      background: var(--iv-surface-2);
      color: var(--iv-text-faint);
      font-size: 11px;
      text-align: center;
    }

    .dashboard-body {
      min-height: 460px;
      display: grid;
      grid-template-columns: 56px 1fr;
    }

    .mock-sidebar {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      padding: 14px 0;
      border-right: 1px solid var(--iv-border);
      background: var(--iv-surface);
    }

    .mark,
    .side-icon {
      display: grid;
      place-items: center;
      color: var(--iv-text-faint);
    }

    .mark {
      width: 26px;
      height: 26px;
      margin-bottom: 14px;
      color: #8b5cf6;
    }

    .mark svg,
    .side-icon svg,
    .module-icon svg,
    .feature-stack svg,
    .contact-lines svg,
    .resend-row svg,
    .check-list svg,
    .plan-card li svg {
      width: 18px;
      height: 18px;
      fill: none;
      stroke: currentColor;
      stroke-linecap: round;
      stroke-linejoin: round;
      stroke-width: 1.8;
    }

    .side-icon {
      width: 32px;
      height: 32px;
      border-radius: 8px;
    }

    .side-icon.active {
      color: #a78bfa;
      background: rgba(79, 70, 229, 0.18);
    }

    .mock-content {
      padding: 20px;
      background: #07090e;
    }

    .mock-head,
    .chart-head,
    .invoice-top,
    .invoice-total,
    .resend-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .mock-head {
      margin-bottom: 16px;
    }

    .mock-head strong {
      display: block;
      color: var(--iv-text);
      font-size: 14px;
      font-weight: 750;
    }

    .mock-head small {
      display: block;
      margin-top: 3px;
      color: var(--iv-text-faint);
      font-size: 10px;
    }

    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      min-height: 26px;
      padding: 0 10px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 800;
    }

    .status-badge::before {
      content: "";
      width: 6px;
      height: 6px;
      border-radius: 999px;
      background: currentColor;
    }

    .status-badge.success {
      border: 1px solid rgba(34, 197, 94, 0.3);
      color: #22c55e;
      background: rgba(34, 197, 94, 0.12);
    }

    .pulse::before {
      animation: badgePulse 1.6s ease infinite;
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 10px;
      margin-bottom: 14px;
    }

    .kpi-card {
      min-width: 0;
      padding: 10px;
      border: 1px solid var(--iv-border);
      border-radius: 8px;
      background: var(--iv-card);
      color: #6d5df2;
    }

    .kpi-card small {
      display: block;
      margin-bottom: 5px;
      color: var(--iv-text-muted);
      font-size: 9.5px;
      font-weight: 700;
    }

    .kpi-card div {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 8px;
    }

    .kpi-card strong {
      color: var(--iv-text);
      font-size: 16px;
      font-weight: 760;
      letter-spacing: -0.02em;
    }

    .kpi-card span {
      color: var(--iv-success);
      font-size: 9px;
      font-weight: 800;
    }

    .kpi-card span.negative {
      color: var(--iv-error);
    }

    .kpi-card svg {
      width: 100%;
      height: 22px;
      display: block;
      margin-top: 6px;
    }

    .analytics-grid {
      display: grid;
      grid-template-columns: 1.6fr 1fr;
      gap: 10px;
    }

    .chart-card {
      min-width: 0;
      padding: 12px;
      border: 1px solid var(--iv-border);
      border-radius: 8px;
      background: var(--iv-card);
    }

    .chart-head {
      margin-bottom: 8px;
    }

    .chart-head strong {
      color: var(--iv-text);
      font-size: 11.5px;
      font-weight: 800;
    }

    .chart-head small {
      color: var(--iv-text-faint);
      font-size: 9px;
    }

    .area-chart {
      width: 100%;
      height: 140px;
      display: block;
    }

    .area-chart line {
      stroke: rgba(148, 163, 184, 0.1);
    }

    .area-chart text {
      fill: var(--iv-text-faint);
      font-family: var(--iv-font-mono);
      font-size: 10px;
    }

    .area-line {
      fill: none;
      stroke: #6d5df2;
      stroke-width: 2.4;
      stroke-linecap: round;
      stroke-linejoin: round;
    }

    .area-line.accent {
      stroke: #a78bfa;
      stroke-dasharray: 5 6;
      stroke-width: 2;
    }

    .bar-chart {
      height: 140px;
      display: flex;
      align-items: end;
      justify-content: center;
      gap: 12px;
      padding: 12px 8px 2px;
      background:
        linear-gradient(rgba(148, 163, 184, 0.09) 1px, transparent 1px) 0 20% / 100% 28px;
    }

    .bar-chart span {
      width: 24px;
      border-radius: 4px 4px 3px 3px;
      background: linear-gradient(180deg, #a78bfa, rgba(109, 93, 242, 0.52));
      box-shadow: 0 0 22px rgba(139, 92, 246, 0.24);
    }

    .iv-section {
      position: relative;
      z-index: 2;
      padding: clamp(96px, 11vw, 148px) 0;
    }

    /* Soft luminous divider between consecutive sections — keeps the
       narrative continuous, no harsh cuts. */
    .iv-section + .iv-section::before {
      content: '';
      position: absolute;
      top: 0;
      left: 50%;
      transform: translateX(-50%);
      width: min(78%, 880px);
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.25), transparent);
      opacity: 0.85;
    }
    /* Diffused fade-in halo at the top of each section so the previous
       section bleeds gently into the next. */
    .iv-section::after {
      content: '';
      position: absolute;
      top: -120px;
      left: 50%;
      transform: translateX(-50%);
      width: min(86%, 1040px);
      height: 240px;
      pointer-events: none;
      z-index: -1;
      background: radial-gradient(ellipse at top, rgba(99, 102, 241, 0.10), transparent 70%);
      filter: blur(32px);
    }

    .section-heading {
      max-width: 740px;
      display: grid;
      justify-items: center;
      gap: 16px;
      margin: 0 auto 56px;
      text-align: center;
    }

    .section-heading h2,
    .section-copy h2 {
      color: var(--iv-text);
      font-size: clamp(32px, 4vw, 48px);
      font-weight: 760;
      line-height: 1.08;
      letter-spacing: -0.03em;
      text-wrap: balance;
    }

    .section-heading p,
    .section-copy p {
      color: var(--iv-text-muted);
      font-size: 16px;
      font-weight: 650;
      line-height: 1.62;
      text-wrap: pretty;
    }

    .module-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 16px;
    }

    .module-card,
    .plan-card,
    .lead-form,
    .invoice-card,
    .live-feed {
      border: 1px solid var(--iv-border);
      background: var(--iv-card);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
    }

    .module-card {
      min-height: 154px;
      display: grid;
      align-content: start;
      gap: 12px;
      padding: 22px;
      border-radius: 10px;
      transition: transform 150ms ease, border-color 150ms ease, background 150ms ease;
    }

    .module-card:hover {
      transform: translateY(-2px);
      border-color: rgba(167, 139, 250, 0.36);
      background: #131c28;
    }

    .module-icon {
      --module-color: #8b5cf6;
      width: 38px;
      height: 38px;
      display: grid;
      place-items: center;
      border: 1px solid color-mix(in srgb, var(--module-color) 40%, transparent);
      border-radius: 10px;
      color: var(--module-color);
      background: color-mix(in srgb, var(--module-color) 13%, transparent);
    }

    .module-card h3 {
      color: var(--iv-text);
      font-size: 15.5px;
      font-weight: 800;
      letter-spacing: -0.012em;
    }

    .module-card p {
      color: var(--iv-text-muted);
      font-size: 13.5px;
      font-weight: 650;
      line-height: 1.5;
    }

    .split-grid {
      display: grid;
      grid-template-columns: 0.86fr 1fr;
      gap: 64px;
      align-items: center;
    }

    .section-copy {
      display: grid;
      gap: 18px;
    }

    .section-copy p {
      max-width: 560px;
    }

    .section-glow {
      width: 760px;
      height: 760px;
      border-radius: 999px;
      filter: blur(132px);
      opacity: 0.18;
      background: radial-gradient(circle, rgba(124, 58, 237, 0.65), transparent 70%);
    }

    .glow-left {
      top: 16%;
      left: -300px;
    }

    .glow-right {
      top: 12%;
      right: -280px;
    }

    .feature-stack {
      display: grid;
      gap: 14px;
      margin-top: 8px;
    }

    .feature-stack article {
      display: grid;
      grid-template-columns: 36px 1fr;
      gap: 12px;
      align-items: center;
    }

    .feature-stack article > span {
      width: 36px;
      height: 36px;
      display: grid;
      place-items: center;
      border: 1px solid rgba(139, 92, 246, 0.32);
      border-radius: 10px;
      color: #a78bfa;
      background: rgba(139, 92, 246, 0.12);
    }

    .feature-stack strong {
      display: block;
      color: var(--iv-text);
      font-size: 14px;
      font-weight: 800;
    }

    .feature-stack small {
      display: block;
      margin-top: 3px;
      color: var(--iv-text-muted);
      font-size: 13px;
      font-weight: 650;
    }

    .live-feed {
      overflow: hidden;
      border-color: var(--iv-border-strong);
      border-radius: 14px;
      box-shadow: 0 24px 80px rgba(0, 0, 0, 0.34), 0 0 80px rgba(79, 70, 229, 0.1);
    }

    .feed-header {
      height: 44px;
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 12px;
      padding: 0 16px;
      border-bottom: 1px solid var(--iv-border);
      background: #121925;
    }

    .live-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 24px;
      padding: 0 9px;
      border: 1px solid rgba(239, 68, 68, 0.28);
      border-radius: 999px;
      color: #ef4444;
      background: rgba(239, 68, 68, 0.1);
      font-size: 11px;
      font-weight: 900;
    }

    .live-pill i {
      width: 7px;
      height: 7px;
      border-radius: 999px;
      background: currentColor;
      box-shadow: 0 0 12px currentColor;
      animation: badgePulse 1.5s ease infinite;
    }

    .feed-header strong {
      color: var(--iv-text-muted);
      font-size: 11px;
    }

    .feed-header small {
      color: var(--iv-text-faint);
      font-size: 10px;
    }

    .feed-frame {
      position: relative;
      aspect-ratio: 16 / 10;
      overflow: hidden;
      background:
        radial-gradient(circle at 20% 82%, rgba(139, 92, 246, 0.28), transparent 24%),
        radial-gradient(circle at 76% 26%, rgba(79, 70, 229, 0.2), transparent 32%),
        linear-gradient(180deg, #111826 0%, #111724 62%, #1b2144 100%);
    }

    .feed-grid {
      position: absolute;
      inset: 18% 10%;
      border: 1px solid rgba(148, 163, 184, 0.08);
      background:
        linear-gradient(rgba(148, 163, 184, 0.04) 1px, transparent 1px),
        linear-gradient(90deg, rgba(148, 163, 184, 0.04) 1px, transparent 1px);
      background-size: 44px 44px;
    }

    .feed-scan {
      position: absolute;
      left: 0;
      right: 0;
      top: 0;
      height: 2px;
      background: linear-gradient(90deg, transparent, rgba(167, 139, 250, 0.9), transparent);
      box-shadow: 0 0 22px rgba(167, 139, 250, 0.7);
      animation: scanFeed 3.1s linear infinite;
    }

    .rec-label,
    .feed-summary {
      position: absolute;
      z-index: 5;
      color: var(--iv-text-faint);
      font-size: 10px;
      font-weight: 800;
    }

    .rec-label {
      top: 14px;
      left: 16px;
    }

    .feed-summary {
      right: 14px;
      bottom: 14px;
    }

    .yolo-box {
      position: absolute;
      z-index: 4;
      display: block;
      border: 2px solid #22c55e;
      animation: boxIn 320ms ease both;
    }

    .yolo-box small {
      position: absolute;
      left: -2px;
      top: -24px;
      display: inline-flex;
      padding: 3px 8px;
      color: #06120b;
      font-family: var(--iv-font-mono);
      font-size: 10px;
      font-weight: 900;
      white-space: nowrap;
    }

    .feed-metrics {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      border-top: 1px solid var(--iv-border);
      background: #101822;
    }

    .feed-metrics article {
      display: grid;
      gap: 3px;
      padding: 12px 16px 14px;
      border-right: 1px solid var(--iv-border);
    }

    .feed-metrics article:last-child {
      border-right: 0;
    }

    .feed-metrics small,
    .invoice-meta small {
      color: var(--iv-text-faint);
      font-size: 10px;
      font-weight: 900;
      letter-spacing: 0.08em;
    }

    .feed-metrics strong {
      color: var(--iv-text);
      font-size: 22px;
      font-weight: 780;
      letter-spacing: -0.03em;
    }

    .invoice-grid {
      grid-template-columns: 1fr 1fr;
    }

    .invoice-card {
      max-width: 440px;
      justify-self: center;
      padding: 24px;
      border-color: var(--iv-border-strong);
      border-radius: 14px;
      box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
    }

    .invoice-top {
      margin-bottom: 18px;
    }

    .invoice-top small,
    .invoice-top strong {
      display: block;
    }

    .invoice-top small {
      color: var(--iv-text-faint);
      font-size: 10.5px;
      letter-spacing: 0.05em;
    }

    .invoice-top strong {
      margin-top: 3px;
      color: var(--iv-text);
      font-size: 18px;
      font-weight: 760;
      letter-spacing: -0.01em;
    }

    .invoice-meta {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
      margin-bottom: 18px;
    }

    .invoice-meta strong {
      display: block;
      margin-top: 4px;
      color: var(--iv-text);
      font-size: 12.5px;
      font-weight: 750;
    }

    .invoice-meta span {
      display: block;
      margin-top: 2px;
      color: var(--iv-text-muted);
      font-size: 11.5px;
    }

    .invoice-lines {
      display: grid;
      gap: 10px;
      padding: 14px 0;
      border-top: 1px solid var(--iv-divider);
      border-bottom: 1px solid var(--iv-divider);
    }

    .invoice-lines div {
      display: flex;
      justify-content: space-between;
      gap: 18px;
      color: var(--iv-text-muted);
      font-size: 12.5px;
      font-weight: 650;
    }

    .invoice-lines strong {
      color: var(--iv-text);
      font-family: var(--iv-font-mono);
      font-size: 12px;
    }

    .invoice-total {
      padding-top: 14px;
      color: var(--iv-text-muted);
      font-size: 13px;
      font-weight: 650;
    }

    .invoice-total strong {
      color: var(--iv-text);
      font-size: 22px;
      font-weight: 850;
      letter-spacing: -0.02em;
    }

    .resend-row {
      margin-top: 16px;
      padding: 12px;
      border: 1px solid var(--iv-border);
      border-radius: 8px;
      background: var(--iv-surface-2);
    }

    .resend-row > span {
      width: 30px;
      height: 30px;
      display: grid;
      place-items: center;
      flex: 0 0 auto;
      border-radius: 8px;
      color: var(--iv-success);
      background: rgba(34, 197, 94, 0.12);
    }

    .resend-row div {
      min-width: 0;
      flex: 1;
    }

    .resend-row strong,
    .resend-row small {
      display: block;
    }

    .resend-row strong {
      color: var(--iv-text);
      font-size: 12px;
      font-weight: 750;
    }

    .resend-row small {
      overflow: hidden;
      color: var(--iv-text-faint);
      font-size: 10.5px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .resend-row b {
      display: inline-flex;
      min-height: 24px;
      align-items: center;
      padding: 0 8px;
      border-radius: 999px;
      color: #c4b5fd;
      background: rgba(139, 92, 246, 0.16);
      font-size: 11px;
    }

    .check-list {
      display: grid;
      gap: 13px;
      margin-top: 8px;
    }

    .check-list span,
    .plan-card li {
      display: flex;
      align-items: center;
      gap: 10px;
      color: var(--iv-text);
      font-size: 14px;
      font-weight: 750;
    }

    .check-list svg,
    .plan-card li svg {
      color: var(--iv-success);
      flex: 0 0 auto;
    }

    .pricing-grid {
      max-width: 1080px;
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 16px;
      margin: 0 auto;
    }

    .plan-card {
      position: relative;
      display: grid;
      align-content: start;
      gap: 16px;
      min-height: 360px;
      padding: 28px;
      border-radius: 16px;
    }

    .plan-card.featured {
      border-color: rgba(79, 70, 229, 0.42);
      background: linear-gradient(180deg, rgba(79, 70, 229, 0.1), var(--iv-card));
      box-shadow: 0 0 70px -18px rgba(79, 70, 229, 0.7);
    }

    .popular {
      position: absolute;
      top: 16px;
      right: 16px;
      padding: 4px 9px;
      border-radius: 999px;
      color: #fff;
      background: #5b4df1;
      font-family: var(--iv-font-mono);
      font-size: 9.5px;
      font-weight: 900;
      letter-spacing: 0.06em;
    }

    .plan-card h3 {
      color: var(--iv-text);
      font-size: 20px;
      font-weight: 820;
      letter-spacing: -0.02em;
    }

    .plan-card p {
      color: var(--iv-text-muted);
      font-size: 13px;
      font-weight: 680;
      line-height: 1.4;
    }

    .plan-price {
      color: var(--iv-text);
      font-size: 22px;
      font-weight: 820;
      letter-spacing: -0.02em;
    }

    .plan-card ul {
      display: grid;
      gap: 11px;
      margin: 6px 0 0;
      padding: 20px 0 0;
      border-top: 1px solid var(--iv-divider);
      list-style: none;
    }

    .plan-card li {
      align-items: flex-start;
      font-size: 13.5px;
      line-height: 1.42;
    }

    .contact-section {
      padding-bottom: 132px;
    }

    .contact-grid {
      display: grid;
      grid-template-columns: minmax(0, 0.82fr) minmax(0, 1.18fr);
      gap: 58px;
      align-items: start;
    }

    .contact-info {
      display: grid;
      gap: 22px;
      align-content: start;
      min-width: 0;
    }

    .contact-info p { margin: 0; }

    .contact-lines {
      display: grid;
      gap: 10px;
      margin-top: 4px;
    }

    .contact-line {
      display: grid;
      grid-template-columns: 44px 1fr;
      align-items: center;
      gap: 14px;
      padding: 12px 14px;
      border: 1px solid var(--iv-border);
      border-radius: 12px;
      background: rgba(15, 20, 28, 0.6);
      color: var(--iv-text);
      text-decoration: none;
      transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
    }
    .contact-line:hover {
      border-color: rgba(167, 139, 250, 0.34);
      background: rgba(15, 20, 28, 0.9);
      transform: translateY(-1px);
    }
    .contact-line .ci {
      width: 44px;
      height: 44px;
      display: grid;
      place-items: center;
      border-radius: 12px;
      color: #a78bfa;
      background: rgba(139, 92, 246, 0.14);
    }
    .contact-line .ci svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; }
    .contact-line .ct {
      display: grid;
      gap: 2px;
      min-width: 0;
    }
    .contact-line .ct small {
      color: var(--iv-text-faint);
      font-size: 10px;
      font-weight: 800;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }
    .contact-line .ct strong {
      color: var(--iv-text);
      font-size: 14px;
      font-weight: 800;
      letter-spacing: -0.005em;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    /* ─── Google Maps embed ─── */
    .map-wrap {
      position: relative;
      overflow: hidden;
      border: 1px solid var(--iv-border-strong);
      border-radius: 16px;
      box-shadow: 0 24px 60px -28px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(167, 139, 250, 0.06) inset;
      aspect-ratio: 16 / 9;
      background: #0a0e16;
      margin-top: 6px;
    }
    .map-iframe {
      width: 100%;
      height: 100%;
      border: 0;
      display: block;
      filter: invert(0.92) hue-rotate(180deg) saturate(0.8) brightness(0.92);
      transition: filter 280ms ease;
    }
    .map-wrap:hover .map-iframe {
      filter: invert(0.86) hue-rotate(180deg) saturate(0.95) brightness(1);
    }
    .map-overlay {
      position: absolute;
      inset: 0;
      pointer-events: none;
      background:
        radial-gradient(ellipse at top right, rgba(139, 92, 246, 0.18), transparent 55%),
        linear-gradient(180deg, transparent 60%, rgba(7, 9, 14, 0.55) 100%);
    }
    .map-cta {
      position: absolute;
      left: 14px;
      bottom: 14px;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      height: 36px;
      padding: 0 14px;
      border: 1px solid rgba(148, 163, 184, 0.22);
      border-radius: 10px;
      color: #fff;
      background: rgba(7, 9, 14, 0.78);
      backdrop-filter: blur(14px);
      -webkit-backdrop-filter: blur(14px);
      font-size: 12.5px;
      font-weight: 800;
      text-decoration: none;
      transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
    }
    .map-cta:hover {
      transform: translateY(-1px);
      border-color: rgba(167, 139, 250, 0.5);
      background: rgba(15, 20, 28, 0.92);
    }
    .map-cta svg { width: 14px; height: 14px; }

    /* ─── Lead form (premium, fieldset-based) ─── */
    .lead-form {
      display: grid;
      gap: 22px;
      padding: 32px;
      border-radius: 18px;
      background:
        linear-gradient(180deg, rgba(139, 92, 246, 0.04), rgba(15, 20, 28, 0.86)),
        rgba(11, 14, 21, 0.85);
      backdrop-filter: blur(18px);
      -webkit-backdrop-filter: blur(18px);
      border: 1px solid var(--iv-border-strong);
      box-shadow:
        0 30px 80px -28px rgba(0, 0, 0, 0.6),
        0 0 90px -40px rgba(79, 70, 229, 0.45),
        inset 0 1px 0 rgba(255, 255, 255, 0.04);
    }

    .lead-form__head {
      display: grid;
      gap: 4px;
    }
    .lead-form__head strong {
      color: var(--iv-text);
      font-size: 18px;
      font-weight: 820;
      letter-spacing: -0.01em;
    }
    .lead-form__head small {
      color: var(--iv-text-muted);
      font-size: 13px;
      font-weight: 600;
    }

    .lead-form fieldset {
      border: 0;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 14px;
    }
    .lead-form legend {
      padding: 0 0 4px;
      color: var(--iv-text-faint);
      font-family: var(--iv-font-mono);
      font-size: 10.5px;
      font-weight: 800;
      letter-spacing: 0.16em;
      text-transform: uppercase;
    }
    .lead-form .row {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
    }
    .lead-form label {
      display: grid;
      gap: 8px;
      min-width: 0;
    }
    .lead-form .wide,
    .lead-form app-request-feedback {
      grid-column: 1 / -1;
    }

    .iv-label {
      color: var(--iv-text-muted);
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 0.005em;
    }

    .iv-input,
    .iv-select,
    .iv-textarea {
      width: 100%;
      border: 1px solid rgba(148, 163, 184, 0.22);
      border-radius: 10px;
      color: var(--iv-text);
      background: rgba(15, 20, 28, 0.72);
      outline: 0;
      transition: border-color 180ms ease, box-shadow 180ms ease, background 180ms ease, transform 180ms ease;
      font-size: 14px;
      font-weight: 600;
    }

    .iv-input,
    .iv-select {
      min-height: 46px;
      padding: 0 14px;
    }

    .iv-input::placeholder,
    .iv-textarea::placeholder {
      color: rgba(148, 163, 184, 0.55);
      font-weight: 500;
    }

    .iv-select {
      appearance: none;
      -webkit-appearance: none;
      background-image: linear-gradient(45deg, transparent 50%, #94a3b8 50%), linear-gradient(135deg, #94a3b8 50%, transparent 50%);
      background-position: calc(100% - 18px) 53%, calc(100% - 13px) 53%;
      background-size: 5px 5px, 5px 5px;
      background-repeat: no-repeat;
      padding-right: 34px;
    }

    .iv-textarea {
      min-height: 116px;
      resize: vertical;
      padding: 12px 14px;
      line-height: 1.55;
    }

    .iv-input:hover,
    .iv-select:hover,
    .iv-textarea:hover {
      border-color: rgba(167, 139, 250, 0.34);
    }
    .iv-input:focus,
    .iv-select:focus,
    .iv-textarea:focus {
      border-color: rgba(167, 139, 250, 0.7);
      box-shadow: 0 0 0 4px rgba(139, 92, 246, 0.18);
      background: rgba(19, 25, 37, 0.9);
    }
    .iv-input:focus,
    .iv-textarea:focus {
      transform: translateY(-1px);
    }

    .form-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 18px;
      margin-top: 6px;
    }

    .form-footer p {
      max-width: 320px;
      color: var(--iv-text-faint);
      font-size: 11.5px;
      font-weight: 700;
      line-height: 1.4;
      margin: 0;
    }

    .landing-footer {
      position: relative;
      z-index: 2;
      border-top: 1px solid var(--iv-border);
      padding: 60px 0 32px;
      background: rgba(7, 9, 14, 0.76);
    }

    .footer-grid {
      display: grid;
      grid-template-columns: 1.3fr 2.7fr;
      gap: 54px;
    }

    .footer-brand {
      display: grid;
      align-content: start;
      gap: 14px;
    }

    .footer-brand p {
      max-width: 280px;
      color: var(--iv-text-muted);
      font-size: 13px;
      font-weight: 650;
      line-height: 1.5;
    }

    .footer-brand small,
    .footer-bottom {
      color: var(--iv-text-faint);
      font-size: 11px;
    }

    .footer-cols {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 28px;
    }

    .footer-cols div {
      display: grid;
      align-content: start;
      gap: 10px;
    }

    .footer-cols strong {
      margin-bottom: 3px;
      color: var(--iv-text);
      font-size: 12px;
      font-weight: 850;
    }

    .footer-cols button {
      width: fit-content;
      padding: 0;
      font-size: 13px;
      font-weight: 700;
      text-align: left;
    }

    .footer-bottom {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      margin-top: 46px;
      padding-top: 24px;
      border-top: 1px solid var(--iv-divider);
      font-weight: 800;
    }

    .iv-fade-up {
      animation: fadeUp 560ms cubic-bezier(0.16, 1, 0.3, 1) both;
    }

    @keyframes fadeUp {
      from {
        opacity: 0;
        transform: translateY(20px);
        filter: blur(10px);
      }
      to {
        opacity: 1;
        transform: none;
        filter: none;
      }
    }

    /* Scroll reveal — applied to .iv-section so each section eases in
       smoothly as it enters the viewport (motion suave tipo SaaS premium). */
    .iv-section {
      opacity: 0;
      transform: translateY(28px);
      transition: opacity 0.85s cubic-bezier(0.22, 1, 0.36, 1),
                  transform 0.85s cubic-bezier(0.22, 1, 0.36, 1);
      will-change: opacity, transform;
    }
    .iv-section.iv-in {
      opacity: 1;
      transform: translateY(0);
    }
    .iv-section.iv-no-reveal {
      opacity: 1;
      transform: none;
      transition: none;
    }

    .module-card,
    .plan-card,
    .feature-stack article,
    .invoice-card,
    .live-feed,
    .resend-row,
    .check-list span {
      transition: transform 200ms cubic-bezier(0.22, 1, 0.36, 1),
                  border-color 200ms ease,
                  box-shadow 200ms ease,
                  background 200ms ease;
    }

    .plan-card:not(.featured):hover {
      transform: translateY(-3px);
      border-color: rgba(167, 139, 250, 0.3);
    }
    .plan-card.featured:hover {
      transform: translateY(-4px);
      box-shadow: 0 0 90px -16px rgba(79, 70, 229, 0.7);
    }

    @keyframes caretBlink {
      0%, 48% { opacity: 1; }
      49%, 100% { opacity: 0; }
    }

    @keyframes badgePulse {
      0%, 100% { opacity: 0.55; box-shadow: 0 0 0 0 currentColor; }
      50% { opacity: 1; box-shadow: 0 0 0 5px transparent; }
    }

    @keyframes scanFeed {
      0% { transform: translateY(0); opacity: 0; }
      12% { opacity: 1; }
      88% { opacity: 1; }
      100% { transform: translateY(330px); opacity: 0; }
    }

    @keyframes boxIn {
      from { opacity: 0; transform: scale(0.96); }
      to { opacity: 1; transform: scale(1); }
    }

    @keyframes drawerIn {
      from { transform: translateX(100%); }
      to { transform: translateX(0); }
    }

    @media (max-width: 1160px) {
      .iv-container {
        width: min(960px, calc(100% - 36px));
      }

      .module-grid,
      .kpi-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      .split-grid,
      .invoice-grid,
      .contact-grid {
        grid-template-columns: 1fr;
      }

      .invoice-card,
      .live-feed {
        width: min(100%, 640px);
        justify-self: center;
      }

      .contact-grid {
        gap: 34px;
      }
    }

    @media (max-width: 920px) {
      .landing-header,
      .header-inner {
        height: 70px;
      }

      .header-inner {
        grid-template-columns: 1fr auto;
        gap: 14px;
      }

      .brand-link img {
        width: clamp(150px, 38vw, 190px);
      }

      .desktop-nav,
      .header-actions .language-pill,
      .header-actions > .iv-btn {
        display: none;
      }

      .menu-button {
        display: grid;
      }

      .hero-section {
        padding-top: 130px;
      }

      .hero-copy h1 {
        font-size: clamp(40px, 10vw, 64px);
      }

      .dashboard-body,
      .analytics-grid,
      .pricing-grid,
      .footer-grid,
      .footer-cols {
        grid-template-columns: 1fr;
      }

      .mock-sidebar {
        display: none;
      }

      .footer-bottom {
        flex-direction: column;
      }
    }

    @media (max-width: 640px) {
      .iv-container {
        width: min(100% - 28px, 560px);
      }

      .landing-header,
      .header-inner {
        height: 64px;
      }

      .brand-link img {
        width: clamp(140px, 44vw, 170px);
      }

      .hero-section,
      .iv-section {
        padding-block: 84px;
      }

      .hero-section {
        padding-top: 116px;
      }

      .hero-copy h1 {
        letter-spacing: -0.04em;
      }

      .typed-word {
        justify-content: center;
        min-width: auto;
        width: 100%;
      }

      .dashboard-wrap {
        margin-top: 42px;
      }

      .dashboard-mockup {
        border-radius: 12px;
        transform: none;
      }

      .browser-bar .address-bar {
        display: none;
      }

      .mock-content {
        padding: 14px;
      }

      .module-grid,
      .kpi-grid,
      .invoice-meta,
      .feed-header {
        grid-template-columns: 1fr;
      }

      .lead-form .row {
        grid-template-columns: 1fr;
      }

      .feed-header {
        height: auto;
        padding: 12px;
      }

      .feed-header small {
        justify-self: start;
      }

      .feed-metrics {
        grid-template-columns: 1fr;
      }

      .feed-metrics article {
        border-right: 0;
        border-bottom: 1px solid var(--iv-border);
      }

      .feed-metrics article:last-child {
        border-bottom: 0;
      }

      .lead-form {
        padding: 20px;
      }

      .form-footer {
        align-items: stretch;
        flex-direction: column;
      }

      .form-footer .iv-btn {
        width: 100%;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .public-site *,
      .public-site *::before,
      .public-site *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        scroll-behavior: auto !important;
        transition-duration: 0.01ms !important;
      }
    }
  `,
})
export class PublicSite implements AfterViewInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly drawerOpen = signal(false);
  protected readonly scrolled = signal(false);
  protected readonly typedText = signal('');
  protected readonly visionBoxes = signal<YoloBox[]>([]);
  protected readonly recClock = signal('00:00');
  protected readonly personCount = signal(0);

  protected readonly lead: LeadPayload = {
    fullName: '',
    companyName: '',
    email: '',
    phone: '',
    city: '',
    businessType: '',
    branchCount: null,
    interest: 'ERP completo',
    message: '',
  };

  /* ─── i18n ─────────────────────────────────────────────────── */
  protected readonly lang = signal<'es' | 'en'>(this.resolveInitialLang());

  private readonly dict: Record<'es' | 'en', Record<string, string>> = {
    es: {
      'city': 'Cuenca',
      'header.home': 'Ir al inicio',
      'header.nav': 'Navegación principal',
      'header.toggleLang': 'Cambiar a inglés',
      'nav.home': 'Inicio',
      'nav.features': 'Funcionalidades',
      'nav.vision': 'Vision AI',
      'nav.invoicing': 'Facturación',
      'nav.pricing': 'Precios',
      'nav.contact': 'Contacto',
      'nav.login': 'Iniciar sesión',
      'nav.demo': 'Solicitar demo',

      'hero.eyebrow': 'ERP + VISION AI · MULTI-TENANT',
      'hero.titleLead': 'La plataforma para controlar',
      'hero.subtitle': 'Inventario, ventas, facturación electrónica SRI y visión artificial con cámaras, en una sola plataforma diseñada para negocios serios.',
      'hero.cta.primary': 'Solicitar demo',
      'hero.cta.secondary': 'Ver funcionalidades',
      'hero.trust': 'Construido para negocios en Ecuador · SRI · Resend · YOLO',

      'mock.summary': 'Resumen en vivo',
      'mock.live': 'En vivo',
      'mock.sales14d': 'Ventas · 14d',

      'modules.eyebrow': 'UNA PLATAFORMA. TODOS LOS MÓDULOS.',
      'modules.title': 'Todo lo que tu operación necesita',
      'modules.subtitle': 'Nueve módulos que se hablan entre sí. Cero hojas de cálculo. Cero parches.',

      'vision.eyebrow': 'EL MÓDULO DIFERENCIADOR',
      'vision.title': 'Vision AI con tus cámaras existentes',
      'vision.subtitle': 'Conecta cámaras IP, recibe detecciones de YOLO en tiempo real y convierte cada frame en una métrica accionable.',
      'vision.branch': 'Sucursal Centro',
      'vision.metric.detections': 'DETECCIONES',
      'vision.metric.confidence': 'CONFIANZA',
      'vision.metric.eventsToday': 'EVENTOS HOY',

      'invoice.label': 'FACTURA',
      'invoice.authorized': 'Autorizado SRI',
      'invoice.client': 'CLIENTE',
      'invoice.date': 'FECHA',
      'invoice.dateValue': '02 May 2026',
      'invoice.totalIva': 'Total con IVA',
      'invoice.sentByResend': 'Enviado por Resend',
      'invoice.delivered': 'entregado',

      'inv.eyebrow': 'FACTURACIÓN ELECTRÓNICA SRI',
      'inv.title': 'Emite, autoriza y entrega — sin pestañas extras',
      'inv.subtitle': 'XML, RIDE PDF, envío automático por correo y reintentos inteligentes. Estado SRI siempre visible.',

      'price.eyebrow': 'PLANES',
      'price.title': 'Crece a tu ritmo',
      'price.subtitle': 'Sin precios sorpresa. Hablamos contigo, entendemos tu operación, te proponemos.',
      'price.cta': 'Solicitar demo',
      'price.popular': 'POPULAR',

      'contact.eyebrow': 'HABLEMOS',
      'contact.title': 'Solicita tu demo',
      'contact.subtitle': 'Esto no crea una cuenta automáticamente. Te contactamos en menos de 24h hábiles para conocer tu negocio.',
      'contact.email': 'Correo',
      'contact.phone': 'Teléfono',
      'contact.location': 'Ubicación',
      'contact.openMap': 'Abrir en Google Maps',

      'form.title': 'Cuéntanos sobre tu operación',
      'form.subtitle': 'Te respondemos en menos de 24h hábiles.',
      'form.section.you': 'Tú',
      'form.section.business': 'Tu negocio',
      'form.section.interest': 'Interés',
      'form.fullName': 'Nombre completo',
      'form.fullName.ph': 'Ej. María Andrade',
      'form.email': 'Correo corporativo',
      'form.phone': 'Teléfono',
      'form.company': 'Empresa',
      'form.company.ph': 'Ej. Constructora Andina',
      'form.city': 'Ciudad',
      'form.businessType': 'Tipo de negocio',
      'form.branches': 'Número de sucursales',
      'form.interest': 'Interés principal',
      'form.message': 'Cuéntanos sobre tu operación',
      'form.message.ph': '¿Qué necesitas operar mejor? ¿Cuántos puntos de venta? ¿Cuántas cámaras?',
      'form.disclaimer': 'No registramos cuentas desde este formulario. Es solo contacto comercial.',
      'form.submit': 'Enviar solicitud',
      'form.sending': 'Enviando…',

      'footer.tagline': 'ERP inteligente con visión artificial para negocios que quieren escalar.',
      'footer.product': 'Producto',
      'footer.support': 'Soporte',
      'footer.company': 'Empresa',
      'footer.legal': 'Legal',
    },
    en: {
      'city': 'Cuenca',
      'header.home': 'Go to home',
      'header.nav': 'Main navigation',
      'header.toggleLang': 'Switch to Spanish',
      'nav.home': 'Home',
      'nav.features': 'Features',
      'nav.vision': 'Vision AI',
      'nav.invoicing': 'Invoicing',
      'nav.pricing': 'Pricing',
      'nav.contact': 'Contact',
      'nav.login': 'Sign in',
      'nav.demo': 'Request demo',

      'hero.eyebrow': 'ERP + VISION AI · MULTI-TENANT',
      'hero.titleLead': 'The platform to run',
      'hero.subtitle': 'Inventory, sales, Ecuadorian e-invoicing and computer vision from your cameras — one platform built for serious businesses.',
      'hero.cta.primary': 'Request demo',
      'hero.cta.secondary': 'See features',
      'hero.trust': 'Built for Ecuadorian businesses · SRI · Resend · YOLO',

      'mock.summary': 'Live overview',
      'mock.live': 'Live',
      'mock.sales14d': 'Sales · 14d',

      'modules.eyebrow': 'ONE PLATFORM. EVERY MODULE.',
      'modules.title': 'Everything your operation needs',
      'modules.subtitle': 'Nine modules that talk to each other. Zero spreadsheets. Zero patches.',

      'vision.eyebrow': 'THE DIFFERENTIATING MODULE',
      'vision.title': 'Vision AI with your existing cameras',
      'vision.subtitle': 'Plug in IP cameras, receive real-time YOLO detections and turn every frame into an actionable metric.',
      'vision.branch': 'Downtown Branch',
      'vision.metric.detections': 'DETECTIONS',
      'vision.metric.confidence': 'CONFIDENCE',
      'vision.metric.eventsToday': 'EVENTS TODAY',

      'invoice.label': 'INVOICE',
      'invoice.authorized': 'SRI Authorized',
      'invoice.client': 'CUSTOMER',
      'invoice.date': 'DATE',
      'invoice.dateValue': 'May 02 2026',
      'invoice.totalIva': 'Total with VAT',
      'invoice.sentByResend': 'Sent via Resend',
      'invoice.delivered': 'delivered',

      'inv.eyebrow': 'ELECTRONIC INVOICING (SRI)',
      'inv.title': 'Issue, authorize and deliver — without extra tabs',
      'inv.subtitle': 'XML, RIDE PDF, automatic email delivery and smart retries. SRI status always visible.',

      'price.eyebrow': 'PLANS',
      'price.title': 'Grow at your pace',
      'price.subtitle': 'No surprise pricing. We talk with you, understand your operation, then propose.',
      'price.cta': 'Request demo',
      'price.popular': 'POPULAR',

      'contact.eyebrow': 'LET\'S TALK',
      'contact.title': 'Book your demo',
      'contact.subtitle': 'This does not create an account automatically. We reach out within 24 business hours to understand your business.',
      'contact.email': 'Email',
      'contact.phone': 'Phone',
      'contact.location': 'Location',
      'contact.openMap': 'Open in Google Maps',

      'form.title': 'Tell us about your operation',
      'form.subtitle': 'We reply within 24 business hours.',
      'form.section.you': 'You',
      'form.section.business': 'Your business',
      'form.section.interest': 'Interest',
      'form.fullName': 'Full name',
      'form.fullName.ph': 'e.g. María Andrade',
      'form.email': 'Work email',
      'form.phone': 'Phone',
      'form.company': 'Company',
      'form.company.ph': 'e.g. Constructora Andina',
      'form.city': 'City',
      'form.businessType': 'Business type',
      'form.branches': 'Number of branches',
      'form.interest': 'Main interest',
      'form.message': 'Tell us about your operation',
      'form.message.ph': 'What do you need to run better? How many POS points? How many cameras?',
      'form.disclaimer': 'We do not create accounts from this form. Sales contact only.',
      'form.submit': 'Send request',
      'form.sending': 'Sending…',

      'footer.tagline': 'Intelligent ERP with computer vision for businesses ready to scale.',
      'footer.product': 'Product',
      'footer.support': 'Support',
      'footer.company': 'Company',
      'footer.legal': 'Legal',
    },
  };

  protected t = (key: string): string => this.dict[this.lang()][key] ?? key;

  protected toggleLang(): void {
    const next = this.lang() === 'es' ? 'en' : 'es';
    this.lang.set(next);
    try { localStorage.setItem('iv-lang', next); } catch { /* ignore */ }
    document.documentElement.setAttribute('lang', next);
  }

  private resolveInitialLang(): 'es' | 'en' {
    try {
      const stored = localStorage.getItem('iv-lang');
      if (stored === 'es' || stored === 'en') return stored;
    } catch { /* ignore */ }
    const navLang = (navigator?.language || 'es').toLowerCase();
    return navLang.startsWith('en') ? 'en' : 'es';
  }

  /* ─── Reactive lists driven by lang() ─────────────────────── */
  protected readonly navItems = computed<NavItem[]>(() => {
    const k = this.lang();
    const labels = this.dict[k];
    return [
      { label: labels['nav.features'],  section: 'features',  route: '/funcionalidades' },
      { label: labels['nav.vision'],    section: 'vision',    route: '/vision-ai' },
      { label: labels['nav.invoicing'], section: 'invoicing', route: '/facturacion-electronica' },
      { label: labels['nav.pricing'],   section: 'pricing',   route: '/precios' },
      { label: labels['nav.contact'],   section: 'contact',   route: '/contacto' },
    ];
  });

  protected readonly sidebarIcons = ['dashboard', 'inventory', 'sales', 'invoice', 'vision', 'reports', 'settings'];
  protected readonly chartGridLines = [28, 68, 108, 148];

  protected readonly modulesList = computed<ModuleCard[]>(() => {
    const es = this.lang() === 'es';
    return [
      { icon: 'inventory', color: '#4f46e5', title: es ? 'Inventario inteligente' : 'Smart inventory',
        copy:  es ? 'Stock en tiempo real, alertas de mínimos, movimientos por sucursal.'
                  : 'Real-time stock, low-level alerts, per-branch movements.' },
      { icon: 'sales', color: '#8b5cf6', title: es ? 'Ventas y órdenes' : 'Sales & orders',
        copy:  es ? 'Pipeline de órdenes, cierres rápidos, comisiones por vendedor.'
                  : 'Order pipeline, fast checkouts, per-seller commissions.' },
      { icon: 'invoice', color: '#22c55e', title: es ? 'Facturación SRI' : 'SRI invoicing',
        copy:  es ? 'XML, RIDE PDF, autorización automática y reintentos.'
                  : 'XML, RIDE PDF, automatic authorization and retries.' },
      { icon: 'vision', color: '#a78bfa', title: 'Vision AI',
        copy:  es ? 'Detección con YOLO, eventos por cámara, métricas accionables.'
                  : 'YOLO detection, per-camera events, actionable metrics.' },
      { icon: 'reports', color: '#38bdf8', title: es ? 'Reportes gerenciales' : 'Management reports',
        copy:  es ? 'Dashboards exportables a CSV/PDF con filtros granulares.'
                  : 'Exportable dashboards (CSV/PDF) with granular filters.' },
      { icon: 'branches', color: '#f59e0b', title: es ? 'Multi-sucursal' : 'Multi-branch',
        copy:  es ? 'Bodegas, sucursales y tenants con permisos finos.'
                  : 'Warehouses, branches and tenants with fine-grained permissions.' },
      { icon: 'shield', color: '#ec4899', title: es ? 'Usuarios y permisos' : 'Users & permissions',
        copy:  es ? 'Roles configurables, auditoría completa y SSO opcional.'
                  : 'Configurable roles, full audit trail and optional SSO.' },
      { icon: 'mail', color: '#ef4444', title: es ? 'Correos con Resend' : 'Email via Resend',
        copy:  es ? 'Envíos transaccionales con logs y reintentos automáticos.'
                  : 'Transactional sends with logs and automatic retries.' },
    ];
  });

  protected readonly visionFeaturesList = computed(() => {
    const es = this.lang() === 'es';
    return [
      { icon: 'camera', title: es ? 'Cámaras IP conectadas' : 'IP cameras connected',
        copy:  es ? 'RTSP, ONVIF y WebRTC. Sin hardware extra.' : 'RTSP, ONVIF and WebRTC. No extra hardware.' },
      { icon: 'cpu',    title: es ? 'YOLO en tiempo real' : 'Real-time YOLO',
        copy:  es ? 'Detección on-prem o cloud, ajustable por cámara.' : 'On-prem or cloud detection, tunable per camera.' },
      { icon: 'zap',    title: es ? 'Eventos accionables' : 'Actionable events',
        copy:  es ? 'Webhooks y notificaciones cuando algo importa.' : 'Webhooks and notifications when something matters.' },
    ];
  });

  protected readonly invoiceLinesList = computed(() => {
    const es = this.lang() === 'es';
    return [
      { qty: 240,  name: es ? 'Cemento Holcim 50kg'      : 'Holcim cement 50kg',  total: '$2040.00' },
      { qty: 80,   name: es ? 'Varilla 12mm 12m'         : 'Rebar 12mm 12m',      total: '$1136.00' },
      { qty: 1200, name: es ? 'Bloque alivianado 20cm'   : 'Lightweight block 20cm', total: '$1140.00' },
    ];
  });

  protected readonly invoiceChecksList = computed(() => {
    const es = this.lang() === 'es';
    return es
      ? ['Autorización SRI automática', 'Entrega por correo (Resend)', 'Reintentos inteligentes en fallos', 'Historial completo y auditable']
      : ['Automatic SRI authorization', 'Email delivery (Resend)', 'Smart retries on failure', 'Complete auditable history'];
  });

  protected readonly plansList = computed<PlanCard[]>(() => {
    const es = this.lang() === 'es';
    return [
      {
        name: 'Starter',
        tagline: es ? 'Para negocios que arrancan' : 'For businesses just starting',
        price:   es ? 'Desde'                       : 'From',
        features: es
          ? ['1 sucursal · 5 usuarios', 'Inventario + Ventas', 'Facturación SRI básica', 'Reportes esenciales', 'Soporte por correo']
          : ['1 branch · 5 users', 'Inventory + Sales', 'Basic SRI invoicing', 'Essential reports', 'Email support'],
      },
      {
        name: 'Business',
        tagline: es ? 'El más popular para retail y distribuidoras' : 'Most popular for retail & distribution',
        price:   es ? 'Consultar precio'                            : 'Ask for pricing',
        featured: true,
        features: es
          ? ['Sucursales ilimitadas', 'Usuarios ilimitados', 'Vision AI · 4 cámaras', 'Reportes avanzados + exports', 'Soporte prioritario']
          : ['Unlimited branches', 'Unlimited users', 'Vision AI · 4 cameras', 'Advanced reports + exports', 'Priority support'],
      },
      {
        name: 'Enterprise',
        tagline: es ? 'Operaciones grandes y multi-marca' : 'Large multi-brand operations',
        price:   es ? 'Personalizado'                     : 'Custom',
        features: es
          ? ['Multi-tenant', 'Vision AI cámaras ilimitadas', 'SSO + auditoría completa', 'API privada + webhooks', 'SLA · soporte 24/7']
          : ['Multi-tenant', 'Unlimited Vision AI cameras', 'SSO + full audit', 'Private API + webhooks', 'SLA · 24/7 support'],
      },
    ];
  });

  protected readonly businessTypesList = computed(() => {
    const es = this.lang() === 'es';
    return es
      ? ['Retail / Tienda', 'Distribuidora / Mayorista', 'Restaurante', 'Construcción', 'Servicios', 'Otro']
      : ['Retail / Store',  'Distribution / Wholesale', 'Restaurant',  'Construction', 'Services',  'Other'];
  });

  protected readonly interestsList = computed(() => {
    const es = this.lang() === 'es';
    return es
      ? ['ERP completo', 'Solo Vision AI', 'Solo facturación SRI', 'Inventario multi-sucursal', 'Reportes / BI']
      : ['Full ERP',     'Only Vision AI', 'Only SRI invoicing',   'Multi-branch inventory',     'Reports / BI'];
  });

  protected readonly footerColumnsList = computed(() => {
    const es = this.lang() === 'es';
    return [
      { title: es ? 'Producto' : 'Product', items: [
        { label: 'Dashboard',                                    section: 'top',       route: '/' },
        { label: 'Vision AI',                                    section: 'vision',    route: '/vision-ai' },
        { label: es ? 'Facturación SRI' : 'SRI invoicing',       section: 'invoicing', route: '/facturacion-electronica' },
        { label: es ? 'Inventario'      : 'Inventory',           section: 'features',  route: '/funcionalidades' },
        { label: es ? 'Reportes'        : 'Reports',             section: 'features',  route: '/funcionalidades' },
      ] },
      { title: es ? 'Soporte' : 'Support', items: [
        { label: es ? 'Centro de ayuda'    : 'Help center',     section: 'contact', route: '/contacto' },
        { label: es ? 'Estado del sistema' : 'System status',   section: 'contact', route: '/contacto' },
        { label: 'API docs',                                    section: 'contact', route: '/contacto' },
        { label: es ? 'Comunidad'         : 'Community',        section: 'contact', route: '/contacto' },
      ] },
      { title: es ? 'Empresa' : 'Company', items: [
        { label: es ? 'Acerca de' : 'About',       section: 'top',      route: '/' },
        { label: es ? 'Clientes'  : 'Customers',   section: 'features', route: '/funcionalidades' },
        { label: 'Blog',                           section: 'pricing',  route: '/precios' },
        { label: es ? 'Contacto'  : 'Contact',     section: 'contact',  route: '/contacto' },
      ] },
      { title: 'Legal', items: [
        { label: es ? 'Términos'   : 'Terms',     section: 'contact', route: '/contacto' },
        { label: es ? 'Privacidad' : 'Privacy',   section: 'contact', route: '/contacto' },
        { label: 'SLA',                           section: 'pricing', route: '/precios' },
        { label: 'DPA',                           section: 'pricing', route: '/precios' },
      ] },
    ];
  });

  /* ─── Live state for the dashboard mockup ─────────────────── */
  private readonly salesSeriesSig = signal<number[]>([320, 410, 290, 480, 540, 620, 580, 720, 690, 810, 780, 900, 950]);
  private readonly eventsSeriesSig = signal<number[]>([72, 168, 270, 372, 228, 330, 420, 510, 360, 552, 468, 630, 720]);
  protected readonly liveBars = signal<number[]>([28, 46, 62, 50, 72, 88, 100]);
  protected readonly liveDetections = signal<number>(1284);
  protected readonly liveConfidence = signal<number>(92);
  protected readonly liveEventsToday = signal<number>(47);
  private readonly liveTick = signal<number>(0);

  private readonly salesSparkSig = signal<number[]>([3, 5, 4, 6, 7, 8, 9]);
  private readonly invoicesSparkSig = signal<number[]>([4, 6, 5, 7, 6, 8, 9]);
  private readonly stockSparkSig = signal<number[]>([9, 8, 7, 6, 5, 6, 7]);
  private readonly aiEventsSparkSig = signal<number[]>([2, 3, 5, 6, 4, 7, 9]);
  private readonly salesValueSig = signal<number>(8.2);  // k$
  private readonly invoicesValueSig = signal<number>(142);
  private readonly stockValueSig = signal<number>(7);
  private readonly aiEventsValueSig = signal<number>(328);

  protected readonly liveDateLabel = computed<string>(() => {
    const now = new Date();
    const day = (this.lang() === 'es'
      ? ['DOM', 'LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB']
      : ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'])[now.getDay()];
    const months = (this.lang() === 'es'
      ? ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC']
      : ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']);
    const dd = String(now.getDate()).padStart(2, '0');
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    const ss = String(now.getSeconds()).padStart(2, '0');
    void this.liveTick(); // re-run every tick
    return `${day} · ${dd} ${months[now.getMonth()]} · ${hh}:${mm}:${ss}`;
  });

  protected readonly visionTickLabel = computed<string>(() => {
    const t = this.liveTick();
    return `T-${String(t % 99).padStart(2, '0')}`;
  });

  protected readonly liveMetrics = computed(() => {
    const es = this.lang() === 'es';
    void this.liveTick();
    return [
      {
        key: 'sales',
        label: es ? 'Ventas hoy' : 'Sales today',
        prefix: '$',
        suffix: 'k',
        display: this.salesValueSig().toFixed(1),
        delta: '+12%',
        negative: false,
        sparkPath: this.toSparkPath(this.salesSparkSig()),
        sparkArea: this.toSparkArea(this.salesSparkSig()),
      },
      {
        key: 'invoices',
        label: es ? 'Facturas' : 'Invoices',
        prefix: '',
        suffix: '',
        display: String(this.invoicesValueSig()),
        delta: '+8%',
        negative: false,
        sparkPath: this.toSparkPath(this.invoicesSparkSig()),
        sparkArea: this.toSparkArea(this.invoicesSparkSig()),
      },
      {
        key: 'stock',
        label: es ? 'Stock bajo' : 'Low stock',
        prefix: '',
        suffix: '',
        display: String(this.stockValueSig()),
        delta: '-2',
        negative: true,
        sparkPath: this.toSparkPath(this.stockSparkSig()),
        sparkArea: this.toSparkArea(this.stockSparkSig()),
      },
      {
        key: 'ai',
        label: es ? 'Eventos AI' : 'AI events',
        prefix: '',
        suffix: '',
        display: String(this.aiEventsValueSig()),
        delta: '+24%',
        negative: false,
        sparkPath: this.toSparkPath(this.aiEventsSparkSig()),
        sparkArea: this.toSparkArea(this.aiEventsSparkSig()),
      },
    ];
  });

  protected readonly liveAreaSales = computed(() => {
    void this.liveTick();
    return this.areaPath(this.salesSeriesSig(), 'sales');
  });
  protected readonly liveAreaEvents = computed(() => {
    void this.liveTick();
    return this.areaPath(this.eventsSeriesSig(), 'events');
  });
  protected readonly liveYAxis = computed<string[]>(() => {
    void this.liveTick();
    const all = [...this.salesSeriesSig(), ...this.eventsSeriesSig()];
    const max = Math.max(...all);
    const min = Math.min(...all);
    const step = (max - min) / 3;
    return [
      Math.round(max).toString(),
      Math.round(max - step).toString(),
      Math.round(max - step * 2).toString(),
      Math.round(min).toString(),
    ];
  });

  private toSparkPath(values: number[]): string {
    if (!values.length) return '';
    const w = 80;
    const h = 18;
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || 1;
    const stepX = w / (values.length - 1);
    const points = values.map((v, i) => `${(i * stepX).toFixed(1)},${(h - ((v - min) / range) * h + 2).toFixed(1)}`);
    return `M2,${(h + 2).toFixed(1)} L ${points.join(' L ')}`;
  }
  private toSparkArea(values: number[]): string {
    const path = this.toSparkPath(values);
    if (!path) return '';
    return `${path} L 80,22 L 2,22 Z`;
  }

  private areaPath(values: number[], _key: 'sales' | 'events'): { line: string; area: string } {
    if (!values.length) return { line: '', area: '' };
    const padL = 42, padR = 15, padT = 16, padB = 22;
    const w = 560 - padL - padR;
    const h = 180 - padT - padB;
    const all = [...this.salesSeriesSig(), ...this.eventsSeriesSig()];
    const min = Math.min(...all);
    const max = Math.max(...all);
    const range = max - min || 1;
    const stepX = w / (values.length - 1);
    const pts: [number, number][] = values.map((v, i) => [
      padL + i * stepX,
      padT + h - ((v - min) / range) * h,
    ]);
    let d = `M ${pts[0][0].toFixed(2)},${pts[0][1].toFixed(2)}`;
    for (let i = 1; i < pts.length; i++) {
      const [x0, y0] = pts[i - 1];
      const [x1, y1] = pts[i];
      const cx = (x0 + x1) / 2;
      d += ` C ${cx.toFixed(2)},${y0.toFixed(2)} ${cx.toFixed(2)},${y1.toFixed(2)} ${x1.toFixed(2)},${y1.toFixed(2)}`;
    }
    const area = `${d} L ${(padL + w).toFixed(2)},${(padT + h).toFixed(2)} L ${padL.toFixed(2)},${(padT + h).toFixed(2)} Z`;
    return { line: d, area };
  }

  private readonly routeSections: Record<string, string> = {
    '/': 'top',
    '/funcionalidades': 'features',
    '/vision-ai': 'vision',
    '/facturacion-electronica': 'invoicing',
    '/precios': 'pricing',
    '/demo': 'contact',
    '/contacto': 'contact',
  };
  private readonly heroWordsEs = ['Inventario.', 'Ventas.', 'Vision AI.', 'Facturación SRI.', 'Reportes.', 'Sucursales.'];
  private readonly heroWordsEn = ['Inventory.', 'Sales.', 'Vision AI.', 'SRI invoicing.', 'Reports.', 'Branches.'];
  private heroWordsList(): string[] { return this.lang() === 'es' ? this.heroWordsEs : this.heroWordsEn; }
  private readonly yoloLabels = ['person', 'cart', 'bottle', 'box', 'person'];
  private typingTimer: number | null = null;
  private typingWordIndex = 0;
  private typingDeleting = false;
  private visionInterval: number | null = null;
  private clockInterval: number | null = null;
  private liveDashboardInterval: number | null = null;
  private feedSeconds = 0;
  private boxSeed = 0;
  private previousBodyBackground = '';
  private previousHtmlBackground = '';
  private revealObserver: IntersectionObserver | null = null;

  @ViewChild('pageRef') private readonly pageRef?: ElementRef<HTMLElement>;

  ngAfterViewInit(): void {
    this.applyDocumentBackground();
    this.onWindowScroll();
    this.scrollToCurrentRoute();
    document.documentElement.setAttribute('lang', this.lang());
    this.startTypewriter();
    this.refreshVisionBoxes();
    this.visionInterval = window.setInterval(() => this.refreshVisionBoxes(), 1500);
    this.clockInterval = window.setInterval(() => this.tickClock(), 1000);
    this.liveDashboardInterval = window.setInterval(() => this.tickLiveDashboard(), 2000);
    this.setupSectionReveals();
  }

  ngOnDestroy(): void {
    this.restoreDocumentBackground();
    this.clearTypingTimer();
    if (this.visionInterval !== null) {
      window.clearInterval(this.visionInterval);
      this.visionInterval = null;
    }
    if (this.clockInterval !== null) {
      window.clearInterval(this.clockInterval);
      this.clockInterval = null;
    }
    if (this.liveDashboardInterval !== null) {
      window.clearInterval(this.liveDashboardInterval);
      this.liveDashboardInterval = null;
    }
    if (this.revealObserver) {
      this.revealObserver.disconnect();
      this.revealObserver = null;
    }
  }

  /* Avanza ligeramente las series y los KPIs para que el dashboard se sienta en vivo. */
  private tickLiveDashboard(): void {
    const stepSeries = (arr: number[]): number[] => {
      const next = arr.slice(1);
      const last = arr[arr.length - 1];
      const drift = (Math.random() - 0.4) * 80;
      const value = Math.max(80, Math.round(last + drift));
      next.push(value);
      return next;
    };
    const stepSpark = (arr: number[]): number[] => {
      const next = arr.slice(1);
      const last = arr[arr.length - 1];
      const v = Math.max(1, Math.min(10, last + (Math.random() < 0.5 ? -1 : 1)));
      next.push(v);
      return next;
    };

    this.salesSeriesSig.set(stepSeries(this.salesSeriesSig()));
    this.eventsSeriesSig.set(stepSeries(this.eventsSeriesSig()));
    this.salesSparkSig.set(stepSpark(this.salesSparkSig()));
    this.invoicesSparkSig.set(stepSpark(this.invoicesSparkSig()));
    this.stockSparkSig.set(stepSpark(this.stockSparkSig()));
    this.aiEventsSparkSig.set(stepSpark(this.aiEventsSparkSig()));

    this.salesValueSig.update((v) => Math.round((v + (Math.random() * 0.3 - 0.05)) * 10) / 10);
    this.invoicesValueSig.update((v) => Math.max(120, v + (Math.random() < 0.65 ? 1 : 0)));
    this.aiEventsValueSig.update((v) => v + Math.floor(Math.random() * 4));
    this.stockValueSig.update((v) => Math.max(2, Math.min(14, v + (Math.random() < 0.5 ? -1 : 1))));

    this.liveBars.set(this.liveBars().map((b) => Math.max(18, Math.min(100, Math.round(b + (Math.random() * 24 - 12))))));

    this.liveDetections.update((v) => v + Math.floor(1 + Math.random() * 5));
    this.liveConfidence.set(Math.round((90 + Math.random() * 8) * 10) / 10);
    this.liveEventsToday.update((v) => v + (Math.random() < 0.4 ? 1 : 0));

    this.liveTick.update((v) => v + 1);
  }

  private setupSectionReveals(): void {
    const root = this.pageRef?.nativeElement;
    if (!root) return;
    const sections = Array.from(root.querySelectorAll<HTMLElement>('.iv-section'));
    if (sections.length === 0) return;

    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduced || typeof IntersectionObserver === 'undefined') {
      sections.forEach((s) => s.classList.add('iv-in', 'iv-no-reveal'));
      return;
    }

    this.revealObserver = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('iv-in');
          this.revealObserver?.unobserve(entry.target);
        }
      });
    }, { root, threshold: 0.08, rootMargin: '0px 0px -8% 0px' });

    sections.forEach((section) => this.revealObserver?.observe(section));
  }

  @HostListener('window:scroll')
  protected onWindowScroll(): void {
    this.scrolled.set(window.scrollY > 12);
  }

  protected onPageScroll(event: Event): void {
    this.scrolled.set((event.currentTarget as HTMLElement).scrollTop > 12);
  }

  protected canSubmit(): boolean {
    return this.lead.fullName.trim().length >= 3 && this.lead.email.trim().includes('@') && !this.submitting();
  }

  protected goToSection(section: string, route: string): void {
    this.drawerOpen.set(false);
    if (this.currentPath() === route) {
      this.deferScroll(section);
      return;
    }
    void this.router.navigateByUrl(route).then(() => this.deferScroll(section));
  }

  protected async submitLead(): Promise<void> {
    if (!this.canSubmit()) {
      this.feedback.set(this.httpFeedback.warning('Ingresa nombre y correo para solicitar la demo.'));
      return;
    }
    this.submitting.set(true);
    this.feedback.set(null);
    try {
      await firstValueFrom(this.http.post(`${API_BASE_URL}/public/leads`, this.payload()));
      this.feedback.set(this.httpFeedback.success('Solicitud recibida. Te contactaremos para coordinar la demo.'));
      this.resetLead();
    } catch (error) {
      const fallback = 'No pudimos registrar la solicitud. Intenta nuevamente o escribe a soporte.';
      this.feedback.set(error instanceof HttpErrorResponse
        ? this.httpFeedback.fromError(error, fallback)
        : this.httpFeedback.fromError(error, fallback));
    } finally {
      this.submitting.set(false);
    }
  }

  private startTypewriter(): void {
    this.clearTypingTimer();
    this.typedText.set('');
    this.typingWordIndex = 0;
    this.typingDeleting = false;
    this.queueTyping(240);
  }

  private advanceTypewriter(): void {
    const words = this.heroWordsList();
    const word = words[this.typingWordIndex % words.length];
    const current = this.typedText();

    if (!this.typingDeleting) {
      if (current.length < word.length) {
        this.typedText.set(word.slice(0, current.length + 1));
        this.queueTyping(60);
        return;
      }
      this.typingDeleting = true;
      this.queueTyping(1200);
      return;
    }

    if (current.length > 0) {
      this.typedText.set(current.slice(0, -1));
      this.queueTyping(28);
      return;
    }

    this.typingDeleting = false;
    this.typingWordIndex = (this.typingWordIndex + 1) % words.length;
    this.queueTyping(140);
  }

  private queueTyping(delay: number): void {
    this.typingTimer = window.setTimeout(() => this.advanceTypewriter(), delay);
  }

  private clearTypingTimer(): void {
    if (this.typingTimer !== null) {
      window.clearTimeout(this.typingTimer);
      this.typingTimer = null;
    }
  }

  private refreshVisionBoxes(): void {
    const colors = ['#22c55e', '#a78bfa', '#38bdf8'];
    const count = 2 + Math.floor(this.random() * 3);
    const boxes = Array.from({ length: count }, (_, index) => {
      const label = this.yoloLabels[Math.floor(this.random() * this.yoloLabels.length)];
      const color = colors[index % colors.length];
      return {
        id: this.boxSeed * 10 + index,
        x: 6 + this.random() * 68,
        y: 18 + this.random() * 52,
        width: 14 + this.random() * 18,
        height: 22 + this.random() * 22,
        label,
        confidence: (0.78 + this.random() * 0.21).toFixed(2),
        color,
      };
    });
    this.boxSeed += 1;
    this.visionBoxes.set(boxes);
    this.personCount.set(boxes.filter((box) => box.label === 'person').length || 1);
  }

  private tickClock(): void {
    this.feedSeconds += 1;
    const minutes = Math.floor(this.feedSeconds / 60).toString().padStart(2, '0');
    const seconds = (this.feedSeconds % 60).toString().padStart(2, '0');
    this.recClock.set(`${minutes}:${seconds}`);
  }

  private random(): number {
    const x = Math.sin(this.boxSeed * 37.17 + this.feedSeconds * 11.31 + Math.random() * 100) * 10000;
    return x - Math.floor(x);
  }

  private applyDocumentBackground(): void {
    this.previousBodyBackground = document.body.style.background;
    this.previousHtmlBackground = document.documentElement.style.background;
    document.body.style.background = '#07090e';
    document.documentElement.style.background = '#07090e';
  }

  private restoreDocumentBackground(): void {
    document.body.style.background = this.previousBodyBackground;
    document.documentElement.style.background = this.previousHtmlBackground;
  }

  private scrollToCurrentRoute(): void {
    const section = this.routeSections[this.currentPath()];
    if (section) {
      this.deferScroll(section);
    }
  }

  private deferScroll(section: string): void {
    window.setTimeout(() => {
      document.getElementById(section)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  }

  private currentPath(): string {
    return window.location.pathname.replace(/\/$/, '') || '/';
  }

  private payload(): LeadPayload {
    return {
      fullName: this.lead.fullName.trim(),
      companyName: this.clean(this.lead.companyName),
      email: this.lead.email.trim().toLowerCase(),
      phone: this.clean(this.lead.phone),
      city: this.clean(this.lead.city),
      businessType: this.clean(this.lead.businessType),
      branchCount: this.lead.branchCount || null,
      interest: this.clean(this.lead.interest),
      message: this.clean(this.lead.message),
    };
  }

  private resetLead(): void {
    this.lead.fullName = '';
    this.lead.companyName = '';
    this.lead.email = '';
    this.lead.phone = '';
    this.lead.city = '';
    this.lead.businessType = '';
    this.lead.branchCount = null;
    this.lead.interest = 'ERP completo';
    this.lead.message = '';
  }

  private clean(value: string | undefined): string | undefined {
    const cleaned = value?.trim();
    return cleaned ? cleaned : undefined;
  }
}
