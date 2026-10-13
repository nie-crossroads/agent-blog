<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BrandGithub from '@vicons/tabler/es/BrandGithub'
import ChevronDown from '@vicons/tabler/es/ChevronDown'
import Menu2 from '@vicons/tabler/es/Menu2'
import Moon from '@vicons/tabler/es/Moon'
import Settings from '@vicons/tabler/es/Settings'
import Sun from '@vicons/tabler/es/Sun'
import { siteConfig } from '@/config'
import { PRESET_COLORS, useThemeStore } from '@/stores/theme'

const theme = useThemeStore()
const route = useRoute()
const settingsOpen = ref(false)
const mobileOpen = ref(false)
const openMenu = ref<string | null>(null)
const menuLocked = ref(false)
const settingWrap = ref<HTMLElement | null>(null)

function active(path: string) {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(`${path}/`)
}

function groupOn(paths: string[]) {
  return paths.some((path) => active(path))
}

function enterMenu(key: string) {
  if (!menuLocked.value) openMenu.value = key
}

function leaveMenu() {
  openMenu.value = null
  menuLocked.value = false
}

function closeOverlays() {
  openMenu.value = null
  mobileOpen.value = false
  settingsOpen.value = false
}

function pickMenu() {
  closeOverlays()
  menuLocked.value = true
}

function toggleSettings(event: MouseEvent) {
  event.stopPropagation()
  settingsOpen.value = !settingsOpen.value
}

function onDocClick(event: Event) {
  if (!settingsOpen.value) return
  const el = settingWrap.value
  if (el && !el.contains(event.target as Node)) {
    settingsOpen.value = false
  }
}

watch(() => route.path, closeOverlays)

onMounted(() => document.addEventListener('pointerdown', onDocClick))
onUnmounted(() => document.removeEventListener('pointerdown', onDocClick))
</script>

<template>
  <header class="navbar">
    <RouterLink to="/" class="brand">{{ siteConfig.engTitle }}</RouterLink>

    <nav class="links">
      <RouterLink to="/" class="item" :class="{ on: active('/') }">首页</RouterLink>

      <div class="drop" :class="{ open: openMenu === 'article' }" @mouseenter="enterMenu('article')" @mouseleave="leaveMenu">
        <span class="item" :class="{ on: groupOn(['/archive', '/category']) }">
          文章 <ChevronDown class="chev" />
        </span>
        <div class="menu card-base">
          <RouterLink to="/archive" class="item" :class="{ on: active('/archive') }" @click="pickMenu">归档</RouterLink>
          <RouterLink to="/category" class="item" :class="{ on: active('/category') }" @click="pickMenu">分类</RouterLink>
        </div>
      </div>

      <div class="drop" :class="{ open: openMenu === 'social' }" @mouseenter="enterMenu('social')" @mouseleave="leaveMenu">
        <span class="item" :class="{ on: groupOn(['/messages', '/friends']) }">
          社交 <ChevronDown class="chev" />
        </span>
        <div class="menu card-base">
          <RouterLink to="/messages" class="item" :class="{ on: active('/messages') }" @click="pickMenu">留言板</RouterLink>
          <RouterLink to="/friends" class="item" :class="{ on: active('/friends') }" @click="pickMenu">友链</RouterLink>
        </div>
      </div>

      <div class="drop" :class="{ open: openMenu === 'personal' }" @mouseenter="enterMenu('personal')" @mouseleave="leaveMenu">
        <span class="item" :class="{ on: groupOn(['/essays', '/about']) }">
          个人 <ChevronDown class="chev" />
        </span>
        <div class="menu card-base">
          <RouterLink to="/essays" class="item" :class="{ on: active('/essays') }" @click="pickMenu">动态</RouterLink>
          <RouterLink to="/about" class="item" :class="{ on: active('/about') }" @click="pickMenu">关于</RouterLink>
        </div>
      </div>

      <RouterLink to="/records" class="item" :class="{ on: active('/records') }">网站日志</RouterLink>
    </nav>

    <div class="actions">
      <button class="icon-btn" type="button" :title="theme.dark ? '日间模式' : '夜间模式'" @click="theme.toggle()">
        <Sun v-if="theme.dark" />
        <Moon v-else />
      </button>
      <div ref="settingWrap" class="setting-wrap">
        <button class="icon-btn" type="button" title="显示设置" @click="toggleSettings">
          <Settings />
        </button>
        <div v-show="settingsOpen" class="float-panel card-base" @click.stop>
          <p class="hue-row">主题色</p>
          <div class="presets">
            <button
              v-for="value in PRESET_COLORS"
              :key="value"
              type="button"
              class="swatch"
              :class="{ on: theme.sameColor(theme.color, value) }"
              :style="{ background: value }"
              @click="theme.setColor(value)"
            />
          </div>
          <input
            class="hue"
            type="range"
            min="0"
            max="360"
            :value="theme.hue"
            @input="theme.setHue(Number(($event.target as HTMLInputElement).value))"
          />
        </div>
      </div>
      <a class="icon-btn" :href="siteConfig.github" target="_blank" rel="noreferrer" title="GitHub">
        <BrandGithub />
      </a>
      <button class="icon-btn mobile" type="button" title="菜单" @click="mobileOpen = !mobileOpen">
        <Menu2 />
      </button>
    </div>
  </header>

  <div v-show="mobileOpen" class="mobile-panel card-base">
    <RouterLink to="/" class="item" :class="{ on: active('/') }" @click="pickMenu">首页</RouterLink>
    <p class="group">文章</p>
    <RouterLink to="/archive" class="item sub" :class="{ on: active('/archive') }" @click="pickMenu">归档</RouterLink>
    <RouterLink to="/category" class="item sub" :class="{ on: active('/category') }" @click="pickMenu">分类</RouterLink>
    <p class="group">社交</p>
    <RouterLink to="/messages" class="item sub" :class="{ on: active('/messages') }" @click="pickMenu">留言板</RouterLink>
    <RouterLink to="/friends" class="item sub" :class="{ on: active('/friends') }" @click="pickMenu">友链</RouterLink>
    <p class="group">个人</p>
    <RouterLink to="/essays" class="item sub" :class="{ on: active('/essays') }" @click="pickMenu">动态</RouterLink>
    <RouterLink to="/about" class="item sub" :class="{ on: active('/about') }" @click="pickMenu">关于</RouterLink>
    <RouterLink to="/records" class="item" :class="{ on: active('/records') }" @click="pickMenu">网站日志</RouterLink>
  </div>
</template>

<style scoped lang="scss">
.navbar {
  width: min(var(--page-width), calc(100% - 2rem));
  margin: 0 auto;
  height: 4.5rem;
  padding: 0 1rem;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  column-gap: 1rem;
  font-size: 16px;
  background: var(--card-bg);
  box-shadow: var(--shadow);
  border-radius: 0 0 var(--radius-large) var(--radius-large);
}

.brand {
  justify-self: start;
  font-family: "MutterKrause", Georgia, serif;
  font-size: 1.55rem;
  font-weight: 400;
  color: var(--c-text-1);
  letter-spacing: 0.04em;
  padding: 0.3rem 0.7rem;
  margin-left: -0.7rem;
  border-radius: 0.7rem;
}

.brand:hover {
  background: var(--btn-plain-bg-hover);
}

.links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.35rem;
}

.item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  height: 2.4rem;
  padding: 0 0.95rem;
  border-radius: 0.7rem;
  font-size: 0.95rem;
  letter-spacing: 0.16em;
  white-space: nowrap;
  color: var(--c-text-1);
  cursor: pointer;
}

.item:hover,
.item.on {
  background: var(--btn-plain-bg-hover);
  color: var(--btn-content);
}

.item.muted {
  cursor: default;
  color: var(--c-text-3);
}

.drop {
  position: relative;
}

.menu {
  display: flex;
  position: absolute;
  top: calc(100% - 0.1rem);
  left: 50%;
  transform: translateX(-50%) translateY(-0.35rem);
  min-width: 7.4rem;
  padding: 0.4rem;
  flex-direction: column;
  z-index: 50;
  border: 1px solid var(--primary);
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease, visibility 0.2s ease;
}

.drop.open .menu {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
  transform: translateX(-50%) translateY(0);
}

.menu .item {
  width: 100%;
  justify-content: center;
  letter-spacing: 0.12em;
}

.chev {
  width: 0.9rem;
  height: 0.9rem;
  opacity: 0.7;
  transition: transform 0.2s ease;
}

.drop.open .chev {
  transform: rotate(180deg);
}

.actions {
  justify-self: end;
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.icon-btn {
  width: 2.6rem;
  height: 2.6rem;
  border: none;
  border-radius: 0.75rem;
  background: var(--float-panel-bg);
  color: var(--btn-content);
  box-shadow: var(--shadow);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  :deep(svg) { width: 1.2rem; height: 1.2rem; }
}

.icon-btn:hover { background: var(--btn-plain-bg-hover); }

.setting-wrap { position: relative; }

.float-panel {
  position: absolute;
  right: 0;
  top: calc(100% + 0.5rem);
  width: 13.5rem;
  padding: 0.9rem;
  z-index: 50;
}

.hue-row {
  margin: 0 0 0.5rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--c-text-1);
}

.presets {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.45rem;
  margin-bottom: 0.75rem;
  justify-items: center;
}

.swatch {
  width: 1.35rem;
  height: 1.35rem;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  padding: 0;
}

.swatch.on {
  border-color: var(--c-text-1);
  box-shadow: 0 0 0 2px var(--card-bg);
}

.hue {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 1.5rem;
  background-image: var(--color-selection-bar);
  border-radius: 0.25rem;
}

.hue::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 0.5rem;
  height: 1rem;
  border-radius: 0.125rem;
  background: rgb(255 255 255 / 0.85);
  cursor: pointer;
}

.mobile { display: none; }

.mobile-panel {
  display: none;
  width: min(var(--page-width), calc(100% - 2rem));
  margin: 0 auto 0.75rem;
  padding: 0.5rem;
  flex-direction: column;
  font-size: 16px;
}

.mobile-panel .item {
  width: 100%;
  justify-content: flex-start;
}

.mobile-panel .sub { padding-left: 1.4rem; }

.group {
  margin: 0.45rem 0.65rem 0.15rem;
  font-size: 0.75rem;
  color: var(--c-text-3);
  letter-spacing: 0.12em;
}

@media (max-width: 980px) {
  .navbar { grid-template-columns: 1fr auto; }
  .links { display: none; }
  .mobile { display: inline-flex; }
  .mobile-panel { display: flex; }
}
</style>
