<script setup lang="ts">
import { computed } from 'vue'
import { resolveAvatarUrl } from '@/utils/avatar'

const props = withDefaults(
  defineProps<{
    avatar?: string | null
    name?: string
    size?: 'sm' | 'md' | 'lg'
  }>(),
  { size: 'md' },
)

const src = computed(() => resolveAvatarUrl(props.avatar))
const sizeClass = computed(() =>
  props.size === 'sm' ? 'size-8' : props.size === 'lg' ? 'size-16' : 'size-10',
)
</script>

<template>
  <img
    :src="src"
    :alt="name || 'Avatar'"
    class="rounded-full object-cover ring-1 ring-gray-200"
    :class="sizeClass"
  />
</template>
