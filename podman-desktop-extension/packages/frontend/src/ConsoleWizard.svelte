<script lang="ts">

  import { faPlusCircle } from '@fortawesome/free-solid-svg-icons';
  import { Button, Table, TableColumn, TableRow } from '@podman-desktop/ui-svelte';
  import { streamshubClient } from '/@/api/client';
  import NavPage from '/@/lib/upstream/NavPage.svelte';
  import { router } from 'tinro';

  let submitted: boolean = false;
  let projectName: string;
  let clusterName: string;
  let clusterNamespace: string;
  let clusterListener: string;
  let clusterBootstrap: string;

  function onNameInput(event: Event) {
    projectName = (event.target as HTMLInputElement).value || '';
  }

  function onClusterNameInput(event: Event) {
    clusterName = (event.target as HTMLInputElement).value || '';
  }

  function onClusterNamespaceInput(event: Event) {
    clusterNamespace = (event.target as HTMLInputElement).value || '';
  }

  function onClusterListenerInput(event: Event) {
    clusterListener = (event.target as HTMLInputElement).value || '';
  }

  function onClusterBootstrapInput(event: Event) {
    clusterBootstrap = (event.target as HTMLInputElement).value || '';
  }

  function isReady() {
    return projectName && clusterName && clusterNamespace && clusterListener && clusterBootstrap;
  }

  async function createConsole() {
    if (!isReady()) {
      return;
    }
    submitted = true;
    streamshubClient.createConsole(projectName, {
      consoleApiServiceAccountToken: 'eyJhbGciOiJSUzI1NiIsImtpZCI6IlNzazBGMElDN0pRQWg2Q1RmUUZjWTVQMGYxcjRCMTlCbFVVVWJndVY5OFEifQ.eyJhdWQiOlsiaHR0cHM6Ly9rdWJlcm5ldGVzLmRlZmF1bHQuc3ZjLmNsdXN0ZXIubG9jYWwiXSwiZXhwIjoxNzQ4Njc5OTczLCJpYXQiOjE3MTcxNDM5NzMsImlzcyI6Imh0dHBzOi8va3ViZXJuZXRlcy5kZWZhdWx0LnN2Yy5jbHVzdGVyLmxvY2FsIiwianRpIjoiYjQ4ZTVhOTgtMzI2ZS00ZGViLTlmMTktODRiYTEzN2JhYmU2Iiwia3ViZXJuZXRlcy5pbyI6eyJuYW1lc3BhY2UiOiJrYWZrYSIsInNlcnZpY2VhY2NvdW50Ijp7Im5hbWUiOiJjb25zb2xlLXNlcnZlciIsInVpZCI6ImVmN2JlMTZiLTg2MGQtNDEzMS05ZjBhLTFjNTcyN2U5NDA2MCJ9fSwibmJmIjoxNzE3MTQzOTczLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a2Fma2E6Y29uc29sZS1zZXJ2ZXIifQ.cVwpsYMyyKa6Da5xz-nXRsXvPxyjP8Gi4ydRDktbh5ppY48AGuj421hLK7GISfA5AivmNzPbmoqv-gGkBw2lmiW3a2QbVrxr0xpZXcHLYaN556GzJp-ljcobL48-D9-ab8WeXjUzsRMwZmnOpFNXcR1g_XhR3WKJM3F7mU7jROnkbiXX5r295c0jxB_2Vz49_SNZDSHrKqO3C9DF6_A5LWUXbPh1qfHpfIqoTqeRF-Xy8eaqsrFxmtWjEPnL5Hl6IE-w-5MAn0U5KmWyYDTz3hc3gmrr0TTHUzEuKD167kAaAmCndMkO4EdTss0L2KIqkAPe578T6xL8T0wS6d9GnQ',
      consoleApiKubernetesApiServerUrl: 'https://192.168.49.2:8443',
      consoleUiImage: 'console-ui:local',
      consoleApiImage: 'console-api:local',
    }, {
      clusters: [{
        listener: clusterListener,
        name: clusterName,
        namespace: clusterNamespace,
        properties: {
          'bootstrap.servers': clusterBootstrap,
        },
        producerProperties: {},
        adminProperties: {},
        consumerProperties: {},
      }],
    });
    router.goto('/');
  }

</script>

<NavPage
  lastPage="{{ name: 'Streamshub', path: '/' }}"
  searchEnabled="{false}"
  title={'Create a Streamshub console'}
>
  <svelte:fragment slot="content">
    <div class="flex flex-col w-full">
      <!-- form -->
      <div class="bg-charcoal-800 m-5 pt-5 space-y-6 px-8 sm:pb-6 xl:pb-8 rounded-lg h-fit">
        <div class="w-full">
          <!-- playground name input -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="projectName">Console name</label>
          <input
            aria-label="Console name"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-700"
            disabled="{submitted}"
            id="projectName"
            name="projectName"
            on:input="{onNameInput}"
            placeholder="Use a unique and memorable name"
            required
            type="text" />
        </div>
        <div class="w-full">
          <!-- kafka name -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="projectName">Kafka cluster name</label>
          <input
            aria-label="Kafka cluster name"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-700"
            disabled="{submitted}"
            id="clusterName"
            name="clusterName"
            on:input="{onClusterNameInput}"
            required
            type="text" />
        </div>
        <div class="w-full">
          <!-- kafka namespace -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="projectName">Kafka cluster namespace</label>
          <input
            aria-label="Kafka cluster namespace"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-700"
            disabled="{submitted}"
            id="clusterNamespace"
            name="clusterNamespace"
            on:input="{onClusterNamespaceInput}"
            required
            type="text" />
        </div>
        <div class="w-full">
          <!-- kafka listener -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="projectName">Kafka cluster listener</label>
          <input
            aria-label="Kafka cluster listener"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-700"
            disabled="{submitted}"
            id="clusterListener"
            name="clusterListener"
            on:input="{onClusterListenerInput}"
            required
            type="text" />
        </div>
        <div class="w-full">
          <!-- kafka bootstrap -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="projectName">Kafka bootstrap urls (comma
            separated)</label>
          <input
            aria-label="Bootstrap url"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-700"
            disabled="{submitted}"
            id="clusterBootstrap"
            name="clusterBootstrap"
            on:input="{onClusterBootstrapInput}"
            required
            type="text" />
        </div>
        <footer>
          <div class="w-full flex flex-col">
            <Button
              icon="{faPlusCircle}"
              inProgress="{submitted}"
              on:click="{createConsole}"
              title="Create console">
              Create console
            </Button>
          </div>
        </footer>
      </div>
    </div>
  </svelte:fragment>


</NavPage>
