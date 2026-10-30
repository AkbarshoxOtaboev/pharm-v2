<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { DEFAULT_AVATARS, resolveAvatarUrl } from '@/utils/avatar'

const { t } = useI18n()
const model = defineModel<string>({ default: 'default-1' })

defineProps<{
  allowUpload?: boolean
  /** Primary full-width upload button (profile page) */
  uploadButton?: boolean
}>()

const emit = defineEmits<{ upload: [file: File] }>()

function onFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) emit('upload', file)
  input.value = ''
}
</script>

<template>
  <div class="space-y-3">
    <p class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">
      {{ t('profile.defaultIcon') }}
    </p>
    <div class="flex flex-wrap justify-center gap-2.5">
      <button
        v-for="key in DEFAULT_AVATARS"
        :key="key"
        type="button"
        class="rounded-full p-0.5 transition"
        :class="
          model === key
            ? 'ring-2 ring-brand-500 ring-offset-2 dark:ring-offset-gray-dark'
            : 'ring-1 ring-gray-200 hover:ring-brand-300 dark:ring-gray-700'
        "
        @click="model = key"
      >
        <img :src="resolveAvatarUrl(key)" :alt="key" class="size-11 rounded-full object-cover" />
      </button>
    </div>
    <div v-if="allowUpload">
      <label
        v-if="uploadButton"
        class="mt-2 inline-flex w-full cursor-pointer items-center justify-center gap-2 rounded-lg bg-brand-500 px-4 py-2.5 text-theme-sm font-medium text-white shadow-theme-xs transition hover:bg-brand-600"
      >
        <svg
          class="size-4.5"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M12 16V8" />
          <path d="M8 11L12 7L16 11" />
          <path d="M4 18H20" />
        </svg>
        {{ t('profile.uploadPhoto') }}
        <input type="file" accept="image/*" class="hidden" @change="onFile" />
      </label>
      <div v-else class="flex items-center gap-3">
        <label
          class="inline-flex cursor-pointer items-center rounded-lg border border-gray-200 bg-white px-3 py-2 text-theme-sm text-gray-700 hover:bg-gray-50 dark:border-gray-800 dark:bg-white/5 dark:text-gray-300 dark:hover:bg-white/10"
        >
          {{ t('profile.uploadPhoto') }}
          <input type="file" accept="image/*" class="hidden" @change="onFile" />
        </label>
        <img
          v-if="model && !model.startsWith('default-')"
          :src="resolveAvatarUrl(model)"
          :alt="t('profile.avatar')"
          class="size-12 rounded-full object-cover ring-2 ring-brand-500 ring-offset-2 dark:ring-offset-gray-dark"
        />
      </div>
    </div>
  </div>
</template>
