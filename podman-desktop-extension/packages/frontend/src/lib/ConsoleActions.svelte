<script lang="ts">
  import type { StreamshubConsoleInfo } from '/@shared/src/models/streamshub';
  import { faExternalLink, faDisplay, faTrash, faStop } from '@fortawesome/free-solid-svg-icons';
  import { router } from 'tinro';
  import { streamshubClient } from '../api/client';
  import ListItemButtonIcon from './upstream/ListItemButtonIcon.svelte';

  export let object: StreamshubConsoleInfo;

  // Delete the build
  async function openEmbeddedConsole(): Promise<void> {
    router.goto(`/console/${object.project}/embedded`);
  }

  async function openConsole(): Promise<void> {
    await streamshubClient.openLink(`http://localhost:${object.ui.ports.find(p => p.PublicPort)?.PublicPort}`);
  }

  async function stopConsole(): Promise<void> {
    await streamshubClient.stopConsole(object.project);
  }

  async function deleteConsole(): Promise<void> {
    await streamshubClient.deleteConsole(object.project);
  }
</script>

<ListItemButtonIcon icon="{faDisplay}" onClick="{() => openEmbeddedConsole()}"
                    title="Open console" />
<ListItemButtonIcon icon="{faExternalLink}" onClick="{() => openConsole()}"
                    title="Open console" />
<ListItemButtonIcon icon="{faStop}" onClick="{() => stopConsole()}"
                    title="Open console" />
<ListItemButtonIcon icon="{faTrash}" onClick="{() => deleteConsole()}"
                    title="Open console" />
