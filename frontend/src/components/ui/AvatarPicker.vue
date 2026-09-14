<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { DEFAULT_AVATARS, resolveAvatarUrl } from '@/utils/avatar'

const { t } = useI18n()
const model = defineModel<string>({ default: 'default-1' })

defineProps<{
  allowUpload?: boolean
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
    <p class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">{{ t('profile.avatar') }}</p>
    <div class="flex flex-wrap gap-3">
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
        <img :src="resolveAvatarUrl(key)" :alt="key" class="size-12 rounded-full object-cover" />
      </button>
    </div>
    <div v-if="allowUpload" class="flex items-center gap-3">
      <label
        class="inline-flex cursor-pointer items-center rounded-lg border border-gray-200 bg-white px-3 py-2 text-theme-sm text-gray-700 hover:bg-gray-50 dark:border-gray-800 dark:bg-white/5 dark:text-gray-300 dark:hover:bg-white/10"
      >
        {{ t('profile.uploadPhoto') }}
        <input type="file" accept="image/*" class="hidden" @change="onFile" />
      </label>
      <img
        v-if="model && !model.startsWith('default-')"
        :src="resolveAvatarUrl(model)"
        alt="Uploaded"
        class="size-12 rounded-full object-cover ring-2 ring-brand-500 ring-offset-2 dark:ring-offset-gray-dark"
      />
    </div>
    <p class="text-theme-xs text-gray-400">{{ t('profile.avatarHint') }}</p>
  </div>
</template>
