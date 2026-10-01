<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    data: Array<{ label: string; value: number }>
    height?: number
    format?: 'currency' | 'number'
  }>(),
  { height: 168, format: 'currency' },
)

const max = computed(() => Math.max(...props.data.map((point) => point.value), 1))

const points = computed(() =>
  props.data.map((point, index) => {
    const ratio = point.value / max.value
    // 4px is reserved at the bottom so a zero-value bar still renders as a visible sliver.
    const height = Math.max(4, Math.round(ratio * (props.height - 24)))
    return { ...point, height, index }
  }),
)

function format(value: number): string {
  return props.format === 'currency'
    ? value.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })
    : value.toLocaleString('en-US')
}
</script>

<template>
  <figure class="chart">
    <figcaption class="chart__caption">Last 7 days</figcaption>
    <div class="chart__plot" :style="{ height: `${height}px` }">
      <div v-for="point in points" :key="point.label + point.index" class="chart__column">
        <div class="chart__bar-wrap">
          <div
            class="chart__bar"
            :class="{ 'is-empty': point.value === 0 }"
            :style="{ height: `${point.height}px` }"
            :title="`${point.label}: ${format(point.value)}`"
          />
          <span class="chart__tooltip">{{ format(point.value) }}</span>
        </div>
        <span class="chart__label">{{ point.label }}</span>
      </div>
    </div>
  </figure>
</template>

<style scoped>
.chart {
  margin: 0;
}

.chart__caption {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-muted);
  margin-bottom: var(--space-4);
}

.chart__plot {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);
  padding-bottom: var(--space-5);
  position: relative;
}

.chart__column {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.chart__bar-wrap {
  position: relative;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: 100%;
}

.chart__bar {
  width: min(38px, 100%);
  border-radius: var(--radius-sm) var(--radius-sm) 2px 2px;
  background: var(--color-primary);
  transition: background-color var(--duration-fast) var(--ease-out);
}

.chart__bar.is-empty {
  background: var(--color-border);
}

.chart__bar-wrap:hover .chart__bar {
  background: var(--color-accent);
}

.chart__tooltip {
  position: absolute;
  bottom: 100%;
  margin-bottom: var(--space-2);
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: var(--color-inverse);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity var(--duration-fast) var(--ease-out);
}

.chart__bar-wrap:hover .chart__tooltip {
  opacity: 1;
}

.chart__label {
  font-size: var(--text-xs);
  color: var(--color-muted);
}
</style>
