<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useSessionGuard } from '@/composables/useSession'

useSessionGuard()
</script>

<template>
  <RouterView />
</template>
