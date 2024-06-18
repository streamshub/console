<script lang="ts">
  import { streamshubClient } from '/@/api/client';
  import ConsoleColumnActions from '/@/lib/ConsoleColumnActions.svelte';
  import ConsoleEmptyScreen from '/@/lib/ConsoleEmptyScreen.svelte';
  import ConsoleProjectColumn from '/@/lib/ConsoleProjectColumn.svelte';
  import NavPage from '/@/lib/upstream/NavPage.svelte';
  import { filtered } from '/@/stores/consolesInfo';
  import type { StreamshubConsoleInfo } from '/@shared/src/models/streamshub';
  import { Button, Table, TableColumn, TableRow } from '@podman-desktop/ui-svelte';
  import { onMount } from 'svelte';
  import { router } from 'tinro';

  let consoles: StreamshubConsoleInfoWithSelected[] | undefined = undefined;

  interface StreamshubConsoleInfoWithSelected extends StreamshubConsoleInfo {
    selected: boolean;
  }

  onMount(async () => {
    filtered.subscribe(value => {
      consoles = value.map(console => ({ ...console, selected: false }));
    });
  });

  async function createConsole() {
    router.goto('/wizard');
  }

  async function gotoConsole(port: number): Promise<void> {
    streamshubClient.telemetryLogUsage('nav-console');
    router.goto(`/console/${port}`);
  }

  let selectedItemsNumber: number;
  let table: Table;

  let projectColumn = new TableColumn<StreamshubConsoleInfo>('Project', {
    renderer: ConsoleProjectColumn,
    comparator: (a, b) => a.project.localeCompare(b.project),
  });

  const columns: TableColumn<StreamshubConsoleInfo, StreamshubConsoleInfo | string>[] = [
    projectColumn,
    new TableColumn<StreamshubConsoleInfo>('Actions', {
      align: 'right',
      renderer: ConsoleColumnActions,
      overflow: true,
    }),
  ];

  const row = new TableRow<StreamshubConsoleInfo>({});
</script>

<NavPage loading={consoles === undefined}
         searchEnabled={false}
         title="Streamshub"
>
  <svelte:fragment slot="additional-actions">
    <Button on:click="{() => createConsole()}" title="Create console">Create console</Button>
  </svelte:fragment>

  <div class="flex min-w-full h-full" slot="content">
    {#if consoles}
      <Table
        bind:this="{table}"
        columns="{columns}"
        data="{consoles}"
        defaultSortColumn="Name"
        kind="Running consoles"
        on:update="{() => (consoles = consoles)}"
        row="{row}">
      </Table>

      {#if consoles.length === 0}
        <ConsoleEmptyScreen />
      {/if}
    {/if}
  </div>
</NavPage>
