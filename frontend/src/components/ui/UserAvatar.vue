<script setup lang="ts">
import { computed } from 'vue'
import { resolveAvatarUrl } from '@/utils/avatar'

const props = withDefaults(
  defineProps<{
    avatar?: string | null
    name?: string
    size?: 'sm' | 'md' | 'lg' | 'xl'
  }>(),
  { size: 'md' },
)

const src = computed(() => resolveAvatarUrl(props.avatar))
const sizeClass = computed(() => {
  if (props.size === 'sm') return 'size-8'
  if (props.size === 'lg') return 'size-16'
  if (props.size === 'xl') return 'size-24'
  return 'size-10'
})
</script>

<template>
  <img
    :src="src"
    :alt="name || 'Avatar'"
    class="rounded-full object-cover ring-1 ring-gray-200"
    :class="sizeClass"
  />
</template>
