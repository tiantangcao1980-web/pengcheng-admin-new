<template>
  <CustomFieldsPanel
    entity-type="customer"
    :entity-id="customerId"
    :readonly="readonly"
  />
</template>

<script setup lang="ts">
import CustomFieldsPanel from '@/views/crm/components/CustomFieldsPanel.vue'

withDefaults(
  defineProps<{
    customerId: number | string
    readonly?: boolean
  }>(),
  { readonly: false }
)
</script>
