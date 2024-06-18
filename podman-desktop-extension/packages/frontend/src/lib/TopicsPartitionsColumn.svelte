<script lang="ts">
  import type { ClusterList } from '/@/api/api-kafka';
  import type { TopicList } from '/@/api/api-topics';
  import Link from './Link.svelte';

  export let project: string;
  export let cluster: string;
  export let object: TopicList;

</script>

<div class="text-sm">
  {object.attributes.numPartitions}
</div>
