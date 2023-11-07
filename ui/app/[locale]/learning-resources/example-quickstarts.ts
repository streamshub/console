import {
  pipelineIcon,
  serverlessIcon,
} from "@patternfly/quickstarts/src/data/mocks/json/tour-icons";
import { QuickStart } from "@patternfly/quickstarts/src/utils/quick-start-types";

export const explorePipelinesQuickStart: QuickStart = {
  apiVersion: "console.openshift.io/v1",
  kind: "QuickStarts",
  metadata: {
    name: "explore-pipelines",
  },
  spec: {
    version: 4.7,
    displayName: `Lorem dolor ipsum`,
    durationMinutes: 10,
    icon: pipelineIcon,
    description: `Install the OpenShift® Pipelines Operator to build Pipelines using Tekton.`,
    introduction: `OpenShift® Pipelines is a cloud-native, continuous integration and continuous delivery (CI/CD) solution based on Kubernetes resources. It uses Tekton building blocks to automate deployments across multiple Kubernetes distributions by abstracting away the underlying implementation details.
* OpenShift Pipelines is a serverless CI/CD system that runs pipelines with all the required dependencies in isolated containers.
* They are designed for decentralized teams that work on a microservice-based architecture.
* They are defined using standard Custom Resource Definitions making them extensible and easy to integrate with the existing Kubernetes tools. This enables you to scale on-demand.
* You can use OpenShift Pipelines to build images with Kubernetes tools such as Source-to-Image (S2I), Buildah, Buildpacks, and Kaniko that are portable across any Kubernetes platform.
* You can use the Developer perspective to create and manage pipelines and view logs in your namespaces.
To start using Pipelines, install the OpenShift® Pipelines Operator on your cluster.`,
    tasks: [
      {
        title: `Installing the OpenShift Pipelines Operator`,
        description: `### To install the OpenShift Pipelines Operator:
1. From the **Administrator** perspective in the console navigation panel, click **Operators > OperatorHub**.
2. In the **Filter by keyword** field, type \`OpenShift Pipelines Operator\`.
3. If the tile has an Installed label, the Operator is already installed. Proceed to the next quick start to create a Pipeline.
4. Click the **tile** to open the Operator details.
5. At the top of the OpenShift Pipelines Operator panel that opens, click **Install**.
6. Fill out the Operator subscription form by selecting the channel that matches your OpenShift cluster, and then click **Install**.
7. On the **Installed Operators** page, wait for the OpenShift Pipelines Operator's status to change from **Installing** to **Succeeded**. `,
        review: {
          instructions: `#### To verify that the OpenShift Pipelines Operator is installed:
1. From the **Operators** section of the navigation, go to the **Installed Operators** page.
2. Verify that the **OpenShift Pipelines Operator** appears in the list of Operators.
In the status column, is the status of the OpenShift Pipelines Operator **Succeeded**?`,
          failedTaskHelp: `This task isn’t verified yet. Try the task again, or [read more](https://docs.openshift.com/container-platform/4.6/pipelines/installing-pipelines.html#op-installing-pipelines-operator-in-web-console_installing-pipelines) about this topic.`,
        },
        summary: {
          success: `You have installed the Pipelines Operator!`,
          failed: `Try the steps again.`,
        },
      },
    ],
    conclusion: `You successfully installed the OpenShift Pipelines Operator! If you want to learn how to deploy an application and associate a Pipeline with it, take the Creating a Pipeline quick start.`,
    nextQuickStart: [`install-app-and-associate-pipeline`],
    accessReviewResources: [
      {
        group: "operators.coreos.com",
        resource: "operatorgroups",
        verb: "list",
      },
      {
        group: "packages.operators.coreos.com",
        resource: "packagemanifests",
        verb: "list",
      },
    ],
  },
};

export const exploreServerlessQuickStart: QuickStart = {
  apiVersion: "console.openshift.io/v1",
  kind: "ConsoleQuickStarts",
  metadata: {
    name: "explore-serverless",
  },
  spec: {
    version: 4.7,
    displayName: `Lorem ipsum dolor`,
    durationMinutes: 10,
    icon: serverlessIcon,
    description: `Install the OpenShift Serverless Operator to deploy stateless, event-trigger-based applications.`,
    introduction: `Red Hat® OpenShift® Serverless lets you run stateless, serverless workloads on a single multi-cloud container platform.
Serverless reduces the need to manage infrastructure or perform back-end development. Scaling is automated, and applications can run on any cloud, hybrid, or on-premises environment. Choosing Serverless means simplicity, portability, and efficiency.
Adding OpenShift Serverless to your OpenShift Container Platform cluster is quick and easy. This quick start walks you through the process.`,
    tasks: [
      {
        title: `Lorem ipsum dolor`,
        description: `### To install the Serverless Operator:
1. From the **Administrator** perspective, go to the **OperatorHub** from the **Operators** section of the navigation.
2. In the **Filter by keyword** field, type \`Serverless\`.
3. If the tile has an **Installed** label on it, the Operator is already installed. Proceed to task two.
4. Click the **OpenShift Serverless Operator** tile.
5. At the top of the OpenShift Serverless Operator panel, click **Install**.
6. Verify that the **OpenShift Serverless Operator Update Channel** is set to the latest version, then click **Install**.
7. Wait for the OpenShift Serverless Operator's status to change from **Installing operator** to **Operator installed - Ready for use**.
`,

        review: {
          instructions: `#### To verify that the OpenShift Serverless Operator is installed:
In the Status column of the **Installed Operators** page, is the OpenShift Serverless Operator’s status **Succeeded?**`,
          failedTaskHelp: `This task is incomplete. Try the task again, or [read more](https://docs.openshift.com/container-platform/4.6/serverless/installing_serverless/installing-openshift-serverless.html) about this topic.`,
        },

        summary: {
          success: `You just installed the OpenShift Serverless Operator! Next, we'll install the required Knative Eventing and Knative Serving Custom Resource components for this Operator to run.`,
          failed: `This task is incomplete. Try the task again, or read more about this topic.`,
        },
      },
      {
        title: `Lorem ipsum dolor`,
        description: `Now let’s install the Knative application programming interfaces (APIs) needed to deploy applications and container workloads.
**To create the Knative Serving and Knative Eventing APIs:**
1. Go to the **Installed Operators** page.
2. Click **OpenShift Serverless Operator**.
3. If it does not already exist, create a project called “knative-serving” under the Project list at the top of the page. If it does exist, select the project from the list.
4. Click the Knative Serving link under Provided APIs or, from Knative Serving tile, click **Create Instance**.
5. Click **Create** to create the custom resource.
6. If it does not already exist, create a project called “knative-eventing” under the Project list at the top of the page. If it does exist, select the project from the list.
7. Click the Knative Eventing link under Provided APIs or, from Knative Eventing tile, click **Create Instance**.
8. Click **Create** to create the custom resource.
`,
        review: {
          instructions: `#### To verify that the Knative Serving and Knative Eventing APIs were installed successfully:
Go to the **All Instances** tab of the OpenShift Serverless Operator.
Are the Knative Serving and Knative Eventing resources in the list of instances?
`,
          failedTaskHelp: `This task isn’t verified yet. Try the task again, or [read more](https://docs.openshift.com/container-platform/4.6/serverless/installing_serverless/installing-knative-serving.html#serverless-create-serving-project-web-console_installing-knative-serving) about this topic.`,
        },
        summary: {
          success: `You just created instances of the Knative Service and Knative Eventing resources.`,
          failed: `Check your work to make sure that the Knative Service and Knative Eventing resources were created.`,
        },
      },
    ],
    conclusion: `Your Serverless Operator is ready! If you want to learn how to deploy a serverless application, take the **Exploring Serverless applications** quick start.`,
    nextQuickStart: [`serverless-application`],
    accessReviewResources: [
      {
        group: "operators.coreos.com",
        resource: "operatorgroups",
        verb: "list",
      },
      {
        group: "packages.operators.coreos.com",
        resource: "packagemanifests",
        verb: "list",
      },
    ],
  },
};

export const monitorSampleAppQuickStart: QuickStart = {
  apiVersion: "console.openshift.io/v1",
  kind: "QuickStarts",
  metadata: {
    name: "monitor-sampleapp",
  },
  spec: {
    version: 4.7,
    displayName: "Lorem ipsum dolor",
    durationMinutes: 10,
    icon: serverlessIcon,
    description: `Now that you’ve created a sample application and added health checks, let’s monitor your application.`,
    prerequisites: [
      `You completed the "Getting started with a sample" quick start.`,
    ],
    introduction: `### This quick start shows you how to monitor your sample application.
You should have previously created the **sample-app** application and **nodejs-sample** deployment via the **Get started with a sample** quick start. If you haven't, you may be able to follow these tasks with any existing deployment.`,
    tasks: [
      {
        title: `Viewing the monitoring details of your sample application`,
        description: `### To view the details of your sample application:
1. Go to the project your sample application was created in.
2. In the **</> Developer** perspective, go to **Topology** view.
3. Click on the **nodejs-sample** deployment to view its details.
4. Click on the **Monitoring** tab in the side panel.
You can see context sensitive metrics and alerts in the **Monitoring** tab.`,
        review: {
          instructions: `#### To verify you can view the monitoring information:
1. Do you see a **Metrics** accordion in the side panel?
2. Do you see a **View monitoring dashboard** link in the **Metrics** accordion?
3. Do you see three charts in the **Metrics** accordion: **CPU Usage**, **Memory Usage** and **Receive Bandwidth**?`,
          failedTaskHelp: `This task isn’t verified yet. Try the task again.`,
        },
        summary: {
          success: `You have learned how you can monitor your sample app!`,
          failed: `Try the steps again.`,
        },
      },
      {
        title: `Viewing your project monitoring dashboard`,
        description: `### To view the project monitoring dashboard in the context of **nodejs-sample**:
1. Click on the **View monitoring dashboard** link in the side panel.
2. You can change the **Time Range** and **Refresh Interval** of the dashboard.
3. You can change the context of the dashboard as well by clicking on the drop-down list. Select a specific workload or **All Workloads** to view the dashboard in the context of the entire project.`,
        review: {
          instructions: `#### To verify that you are able to view the monitoring dashboard:
Do you see metrics charts in the dashboard?`,
          failedTaskHelp: `This task isn’t verified yet. Try the task again.`,
        },
        summary: {
          success: `You have learned how to view the dashboard in the context of your sample app!`,
          failed: `Try the steps again.`,
        },
      },
      {
        title: `Viewing custom metrics`,
        description: `### To view custom metrics:
1. Click on the **Metrics** tab of the **Monitoring** page.
2. Click the **Select Query** drop-down list to see the available queries.
3. Click on **Filesystem Usage** from the list to run the query.`,
        review: {
          instructions: `#### Verify you can see the chart associated with the query:
Do you see a chart displayed with filesystem usage for your project?  Note: select **Custom Query** from the dropdown to create and run a custom query utilizing PromQL.
`,
          failedTaskHelp: `This task isn’t verified yet. Try the task again.`,
        },
        summary: {
          success: `You have learned how to run a query!`,
          failed: `Try the steps again.`,
        },
      },
    ],
    conclusion: `You have learned how to access workload monitoring and metrics!`,

    nextQuickStart: [``],
  },
};
