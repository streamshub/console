<script lang="ts">

  import {streamshubClient} from '/@/api/client';
  import NavPage from '/@/lib/upstream/NavPage.svelte';
  import {faPlusCircle} from '@fortawesome/free-solid-svg-icons';
  import {Button} from '@podman-desktop/ui-svelte';
  import {router} from 'tinro';

  let submitted: boolean = false;
  let projectName: string;
  let clusterName: string;
  let clusterNamespace: string;
  let clusterListener: string;
  let clusterBootstrap: string | undefined;
  let clusterSaslJaas: string | undefined;
  let clusterSaslMechanism: string | undefined;
  let clusterSecurityProtocol: string | undefined;
  let k8sApi: string;
  let k8sToken: string | undefined;

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

  function onClusterSaslJaasInput(event: Event) {
    clusterSaslJaas = (event.target as HTMLInputElement).value || '';
  }

  function onClusterSaslMechanism(event: Event) {
    clusterSaslMechanism = (event.target as HTMLInputElement).value || '';
  }

  function onClusterSecurityProtocol(event: Event) {
    clusterSecurityProtocol = (event.target as HTMLInputElement).value || '';
  }

  function onK8sApiInput(event: Event) {
    k8sApi = (event.target as HTMLInputElement).value || '';
  }

  function onK8sTokenInput(event: Event) {
    k8sToken = (event.target as HTMLInputElement).value || '';
  }

  function isReady() {
    return projectName && clusterName && clusterNamespace && clusterListener;
  }

  async function createConsole() {
    if (!isReady()) {
      return;
    }
    submitted = true;
    streamshubClient.createConsole(projectName, {
      consoleApiServiceAccountToken: k8sToken,
      consoleApiKubernetesApiServerUrl: k8sApi,
      consoleUiImage: 'console-ui:local',
      consoleApiImage: 'console-api:local',
    }, {
      clusters: [{
        listener: clusterListener,
        name: clusterName,
        namespace: clusterNamespace,
        properties: {
          'bootstrap.servers': clusterBootstrap,
          "sasl.jaas.config": clusterSaslJaas,
          "sasl.mechanism": clusterSaslMechanism,
          "security.protocol": clusterSecurityProtocol
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
  loading={submitted}
  searchEnabled="{false}"
  title={'Create a Streamshub console'}
>
  <svelte:fragment slot="additional-actions">
    <Button
      icon="{faPlusCircle}"
      inProgress="{submitted}"
      on:click="{createConsole}"
      title="Create console">
      Create console
    </Button>
  </svelte:fragment>

  <svelte:fragment slot="content">
    <form class="flex flex-col w-full">
      <!-- form -->
      <div class="bg-charcoal-800 m-5 pt-5 space-y-6 px-8 sm:pb-6 xl:pb-8 rounded-lg h-fit">
        <div class="w-full">
          <!-- playground name input -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="projectName">Console name</label>
          <input
            aria-label="Console name"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900 invalid:[&:not(:placeholder-shown):not(:focus)]:text-red-600"
            disabled="{submitted}"
            id="projectName"
            name="projectName"
            on:input="{onNameInput}"
            pattern={"[\\w_\\p{Pd}]{3,}"}
            placeholder="Use a unique and memorable name"
            required
            type="text" />
        </div>
        <p class="block mt-2 mb-2 text-sm text-gray-600">This will be used to create a directory on your filesystem in this extension's storage path.
          <br>
          <br>* It must be a unique name.
          <br>* Name must be at least 3 characters long.
          <br>* Only characters and numbers allowed.
        </p>
      </div>

      <div class="bg-charcoal-800 m-5 pt-5 space-y-6 px-8 sm:pb-6 xl:pb-8 rounded-lg h-fit">
        <div class="flex flex-row space-x-2 items-center text-[var(--pd-content-card-header-text)]">
          <p class="text-lg font-semibold">Kubernetes connection details</p>
        </div>
        <div class="w-full">
          <!-- kafka name -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="k8sApi">API server url</label>
          <input
            aria-label="Kubernetes API server url"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="k8sApi"
            name="k8sApi"
            on:input="{onK8sApiInput}"
            required
            type="text" />
        </div>
        <div class="w-full">
          <!-- kafka namespace -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="k8sToken">Service account token</label>
          <input
            aria-label="Kubernetes service account token"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="k8sToken"
            name="k8sToken"
            on:input="{onK8sTokenInput}"
            placeholder="Leave empty to automatically create one"
            type="text" />
        </div>
      </div>

      <div class="bg-charcoal-800 m-5 pt-5 space-y-6 px-8 sm:pb-6 xl:pb-8 rounded-lg h-fit">
            <div class="flex flex-row space-x-2 items-center text-[var(--pd-content-card-header-text)]">
              <p class="text-lg font-semibold">Strimzi CR details</p>
            </div>
        <div class="w-full">
          <!-- kafka name -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="clusterName">Kafka cluster name</label>
          <input
            aria-label="Kafka cluster name"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="clusterName"
            name="clusterName"
            on:input="{onClusterNameInput}"
            required
            type="text" />
          <p class="block mt-2 mb-2 text-sm text-gray-600">Name of the Strimzi Kafka CR</p>
        </div>
        <div class="w-full">
          <!-- kafka namespace -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="clusterNamespace">Kafka cluster namespace</label>
          <input
            aria-label="Kafka cluster namespace"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="clusterNamespace"
            name="clusterNamespace"
            on:input="{onClusterNamespaceInput}"
            required
            type="text" />
          <p class="block mt-2 mb-2 text-sm text-gray-600">Namespace of the Strimzi Kafka CR</p>
        </div>
        <div class="w-full">
          <!-- kafka listener -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="clusterListener">Cluster listener</label>
          <input
            aria-label="Strimzi cluster listener"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="clusterListener"
            name="clusterListener"
            on:input="{onClusterListenerInput}"
            required
            type="text" />
          <p class="block mt-2 mb-2 text-sm text-gray-600">Name of the listener to use for connections from the console</p>
        </div>
      </div>
        <div class="bg-charcoal-800 m-5 pt-5 space-y-6 px-8 sm:pb-6 xl:pb-8 rounded-lg h-fit">
        <div class="flex flex-row space-x-2 items-center text-[var(--pd-content-card-header-text)]">
          <p class="text-lg font-semibold">Kafka connection properties (optional)</p>
        </div>
        <div class="w-full">
          <!-- kafka bootstrap -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="clusterBootstrap">Bootstrap urls (comma
            separated)</label>
          <input
            aria-label="Bootstrap url"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="clusterBootstrap"
            name="clusterBootstrap"
            on:input="{onClusterBootstrapInput}"
            required
            type="text" />
          <p class="block mt-2 mb-2 text-sm text-gray-600">If omitted the bootstrap servers from the Strimzi Kafka CR are used</p>
        </div>
        <div class="w-full">
          <!-- kafka properties -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="clusterSecurityProtocol">Security protocol</label>
          <input
            aria-label="Security protocol"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="clusterSecurityProtocol"
            name="clusterSecurityProtocol"
            on:input="{onClusterSecurityProtocol}"
            placeholder="SASL_SSL"
            type="text" />
        </div>
        <div class="w-full">
          <!-- kafka properties -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="clusterSaslMechanism">Security protocol</label>
          <input
            aria-label="SASL mechanism"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="clusterSaslMechanism"
            name="clusterSaslMechanism"
            on:input="{onClusterSaslMechanism}"
            placeholder="SCRAM-SHA-512"
            type="text" />
        </div>
        <div class="w-full">
          <!-- kafka properties -->
          <label class="block mb-2 text-sm font-bold text-gray-400" for="clusterSecurityProtocol">SASL JAAS configuration</label>
          <input
            aria-label="SASL JAAS configuration"
            class="w-full p-2 outline-none text-sm bg-charcoal-600 rounded-sm text-gray-700 placeholder-gray-900"
            disabled="{submitted}"
            id="clusterSaslJaasConfiguration"
            name="clusterSaslJaasConfiguration"
            on:input="{onClusterSaslJaasInput}"
            placeholder={`org.apache.kafka.common.security.scram.ScramLoginModule required username="my-user" password="123456"; (optional)`}
            type="text" />
        </div>
      </div>

      <footer class="bg-charcoal-800 m-5 space-y-6 p-8 rounded-lg h-fit">
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
    </form>
  </svelte:fragment>


</NavPage>
