package com.github.streamshub.systemtests.locators;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public record ConsoleLocators(Page page) {

    public static ConsoleLocators of(Page page) {
        return new ConsoleLocators(page);
    }

    public Masthead masthead() {
        return new Masthead(page.locator(ouiaId("masthead-toolbar")));
    }

    public Locator globalDataRefresh() {
        return masthead().globalDataRefresh();
    }

    public NodesOverviewPage nodesOverview() {
        return new NodesOverviewPage(page.locator(ouiaId("nodes-overview-page")));
    }

    public NodesRebalancePage nodesRebalance() {
        return new NodesRebalancePage(page.locator(ouiaId("nodes-rebalances-page")));
    }

    public RebalanceDetailPage rebalanceDetail() {
        return new RebalanceDetailPage(page.locator(ouiaId("nodes-rebalances-detail-page")));
    }

    public GroupsPage groups() {
        return new GroupsPage(page.locator(ouiaId("groups-page")));
    }

    public ConnectConnectorsPage connectConnectors() {
        return new ConnectConnectorsPage(page.locator(ouiaId("connect-connectors-page")));
    }

    public ConnectClustersPage connectClusters() {
        return new ConnectClustersPage(page.locator(ouiaId("connect-clusters-page")));
    }

    public record Masthead(Locator locator) {

        public Locator userSessionMenuToggle() {
            return locator.locator(ouiaId("user-session-menu-toggle"));
        }

        public Locator globalDataRefresh() {
            return locator.locator(ouiaId("global-data-refresh"));
        }

        public UserSessionDropdownMenu userSessionMenu() {
            /*
             * The modal is "popped out" from the component context. Therefore,
             * we need to locate it from the page and not within the current locator.
             */
            return new UserSessionDropdownMenu(locator.page().locator(UserSessionDropdownMenu.OUIA_ID));
        }

        public record UserSessionDropdownMenu(Locator locator) {
            static final String OUIA_ID = ouiaId("user-session-menu");

            public Locator logout() {
                return buttonWithin(locator.locator(ouiaId("user-session-logout")));
            }
        }
    }

    /**
     * Nodes Page Model
     */
    public record NodesOverviewPage(Locator locator) {
        public NodesPageTitle title() {
            return new NodesPageTitle(locator.locator(titleOuiaId()));
        }

        public Summary summary() {
            return new Summary(locator.locator(ouiaId("summary")));
        }

        public DataView dataView() {
            return new DataView(locator, "nodes");
        }

        public record Summary(Locator locator) {
            public DescriptionListGroup totalNodeCount() {
                return new DescriptionListGroup(locator.locator(ouiaId("total-node-count")));
            }
            public DescriptionListGroup controllerNodeCount() {
                return new DescriptionListGroup(locator.locator(ouiaId("controller-node-count")));
            }
            public DescriptionListGroup brokerNodeCount() {
                return new DescriptionListGroup(locator.locator(ouiaId("broker-node-count")));
            }
            public DescriptionListGroup leadController() {
                return new DescriptionListGroup(locator.locator(ouiaId("lead-controller")));
            }
        }
    }

    /**
     * Nodes Page Model
     */
    public record NodesRebalancePage(Locator locator) {
        public NodesPageTitle title() {
            return new NodesPageTitle(locator.locator(titleOuiaId()));
        }

        public DataView dataView() {
            return new DataView(locator, "rebalances");
        }

        public RebalanceActionDropdownMenu actionMenu() {
            /*
             * The menu may be "popped out" from the component context. Therefore,
             * we need to locate it from the page and not within the current locator.
             */
            return new RebalanceActionDropdownMenu(locator.page().locator(ouiaId("rebalance-action-dropdown-menu")));
        }

        public RebalanceConfirmationModal confirmationModal() {
            /*
             * The modal is "popped out" from the component context. Therefore,
             * we need to locate it from the page and not within the current locator.
             */
            return new RebalanceConfirmationModal(locator.page().locator(RebalanceConfirmationModal.OUIA_ID));
        }

        public record RebalanceActionDropdownMenu(Locator locator) {
            public Locator approveButton() {
                return buttonWithin(locator).nth(0);
            }

            public Locator refreshButton() {
                return buttonWithin(locator).nth(1);
            }

            public Locator stopButton() {
                return buttonWithin(locator).nth(2);
            }
        }
    }

    /**
     * Node page title model, shared by nodes overview and rebalances
     */
    public record NodesPageTitle(Locator locator) {
        public Locator value() {
            return locator.locator(ouiaId("value"));
        }

        public Locator labelTotal() {
            return locator.locator(ouiaId("label-total"));
        }

        public Locator labelHealthy() {
            return locator.locator(ouiaId("label-healthy"));
        }

        public Locator labelUnhealthy() {
            return locator.locator(ouiaId("label-unhealthy"));
        }
    }

    /**
     * Nodes Detail Page Model
     */
    public record RebalanceDetailPage(Locator locator) {
        public Locator title() {
            return locator.locator(ouiaId("title"));
        }

        public RebalanceConfirmationModal confirmationModal() {
            /*
             * The modal is "popped out" from the component context. Therefore,
             * we need to locate it from the page and not within the current locator.
             */
            return new RebalanceConfirmationModal(locator.page().locator(RebalanceConfirmationModal.OUIA_ID));
        }

        public ProposalDetail proposalDetail() {
            return new ProposalDetail(locator.locator(ouiaId("proposal-detail")));
        }

        public record ProposalDetail(Locator locator) {
            public Locator expansionToggle() {
                return locator.locator("button#rebalance-proposal-detail-toggle");
            }

            public Locator cardAttribute(String name) {
                return locator.locator(ouiaId(name));
            }

            public DescriptionListGroup listAttribute(String name) {
                return new DescriptionListGroup(locator.locator(ouiaId(name)));
            }
        }
    }

    public record RebalanceConfirmationModal(Locator locator) {
        static final String OUIA_ID = ouiaId("rebalance-confirmation-modal");

        public Locator confirm() {
            return locator.locator(ouiaId("confirm"));
        }

        public Locator cancel() {
            return locator.locator(ouiaId("cancel"));
        }
    }

    /**
     * Groups Page Model
     */
    public record GroupsPage(Locator locator) {
        public Locator title() {
            return locator.locator(titleOuiaId());
        }

        public DataView dataView() {
            return new DataView(locator, "kafka-groups");
        }
    }

    /**
     * Kafka Connect :: Connectors Page Model
     */
    public record ConnectConnectorsPage(Locator locator) {
        public Locator title() {
            return locator.locator(titleOuiaId());
        }

        public KafkaConnectTabs tabs() {
            return new KafkaConnectTabs(locator);
        }

        public DataView dataView() {
            return new DataView(locator, "kafka-connectors");
        }
    }

    /**
     * Kafka Connect :: Clusters Page Model
     */
    public record ConnectClustersPage(Locator locator) {
        public Locator title() {
            return locator.locator(titleOuiaId());
        }

        public KafkaConnectTabs tabs() {
            return new KafkaConnectTabs(locator);
        }

        public DataView dataView() {
            return new DataView(locator, "kafka-connect-clusters");
        }
    }

    public record KafkaConnectTabs(Locator locator) {
        public Locator connectors() {
            return locator.locator(ouiaId("connectors-tab"));
        }

        public Locator connectClusters() {
            return locator.locator(ouiaId("connect-clusters-tab"));
        }
    }

    /**
     * Generic model for description list (HTML dl)
     */
    public record DescriptionListGroup(Locator locator) {
        public Locator term() {
            return locator.locator("dt");
        }

        public Locator description() {
            return locator.locator("dd");
        }
    }

    public record DataView(Locator locator, String prefix) {
        public DataViewToolbar toolbar() {
            return new DataViewToolbar(locator.locator(ouiaId(prefix + "-toolbar")), prefix);
        }

        public DataViewTable table() {
            return new DataViewTable(locator.locator(ouiaId(prefix + "-table")));
        }
    }

    public record DataViewToolbar(Locator locator, String prefix) {
        public Locator filtersToggle() {
            return locator.locator(ouiaId(prefix + "-filters-toggle"));
        }

        public Locator filterItem(String text) {
            return locator.locator(ouiaId(prefix + "-filters-menu"))
                    .getByRole(AriaRole.MENUITEM)
                    .getByText(text);
        }

        public DataViewCheckboxFilter checkboxFilter() {
            return new DataViewCheckboxFilter(locator.locator(ouiaId(prefix + "-filter-value")), prefix);
        }

        public DataViewTextFilter textFilter() {
            return new DataViewTextFilter(locator.locator(ouiaId(prefix + "-filter-value")), prefix);
        }

        public Locator clearFilters() {
            return locator.locator(ouiaId(prefix + "-toolbar-clear-all-filters"));
        }
    }

    public record DataViewCheckboxFilter(Locator locator, String prefix) {
        public Locator toggle() {
            return locator.locator(ouiaId(prefix + "-filter-value-toggle"));
        }

        public Locator menu() {
            /*
             * The menu may be "popped out" from the component context. Therefore,
             * we need to locate it from the page and not within the current locator.
             */
            return locator.page().locator(ouiaId(prefix + "-filter-value-menu"));
        }

        public Locator checkbox(String text) {
            return menu()
                    .getByRole(AriaRole.MENUITEM)
                    .filter(new Locator.FilterOptions().setHasText(text))
                    .locator("input");
        }
    }

    public record DataViewTextFilter(Locator locator, String prefix) {
        public Locator input() {
            return locator.locator("input" + attributeSelector("type", "=", "text"));
        }

        public Locator submit() {
            return locator.locator("button" + attributeSelector("type", "=", "submit"));
        }
    }

    public record DataViewTable(Locator locator) {
        public DataViewTableHead head() {
            return new DataViewTableHead(locator.locator("thead"));
        }

        public DataViewTableBody body() {
            return new DataViewTableBody(locator.locator("tbody"));
        }
    }

    public record DataViewTableHead(Locator locator) {
        public DataViewTableHeadRow rows() {
            return new DataViewTableHeadRow(locator.locator("tr"));
        }
    }

    public record DataViewTableHeadRow(Locator locator) {
        public Locator cells() {
            return locator.locator("th");
        }
    }

    public record DataViewTableBody(Locator locator) {
        /**
         * Retrieves a DataViewTableBodyRow locating all rows, both
         * control and expandable content if this table contains expandable rows.
         */
        public DataViewTableBodyRow rows() {
            return new DataViewTableBodyRow(locator.locator("tr"));
        }

        /**
         * Retrieves a DataViewTableBodyRow locating a single row that includes both
         * control and expandable content if this table contains expandable rows.
         */
        public DataViewTableBodyRow row(int row) {
            return new DataViewTableBodyRow(locator.locator("tr").nth(row));
        }

        /**
         * Retrieves a DataViewTableBodyRow locating only control rows. This is
         * relevant only if this table contains expandable rows.
         */
        public DataViewTableBodyRow controlRows() {
            return new DataViewTableBodyRow(locator.locator("tr" + classMatcher("*=", "control-row")));
        }

        /**
         * Retrieves a DataViewTableBodyRow locating a single control row. This is
         * relevant only if this table contains expandable rows.
         */
        public DataViewTableBodyRow controlRow(int row) {
            return new DataViewTableBodyRow(locator.locator("tr" + classMatcher("*=", "control-row")).nth(row));
        }

        /**
         * Retrieves a DataViewTableBodyRow locating only expandable rows. This is
         * relevant only if this table contains expandable rows.
         */
        public DataViewTableBodyRow expandableRows() {
            return new DataViewTableBodyRow(locator.locator("tr" + classMatcher("*=", "expandable-row")));
        }

        /**
         * Retrieves a DataViewTableBodyRow locating a single expandable row. This is
         * relevant only if this table contains expandable rows.
         */
        public DataViewTableBodyRow expandableRow(int row) {
            return new DataViewTableBodyRow(locator.locator("tr" + classMatcher("*=", "expandable-row")).nth(row));
        }

        public Locator cell(int row, String dataLabel) {
            return row(row).cell(dataLabel);
        }
    }

    public record DataViewTableBodyRow(Locator locator) {
        /**
         * When this DataViewTableBodyRow represents a selection of multiple rows,
         * this method will return a new DataViewTableBodyRow locating a single
         * row as indicated by the zero-based parameter.
         */
        public DataViewTableBodyRow nth(int row) {
            return new DataViewTableBodyRow(locator.nth(row));
        }

        /**
         * When this DataViewTableBodyRow is the control row for an expandable row,
         * this method will locate the expansion button in the first cell. This 
         * method is not relevant for tables without expandable rows.
         */
        public Locator expansionToggle() {
            return buttonWithin(cells().first());
        }

        /**
         * When this DataViewTableBodyRow is a row with an action menu, this method
         * will locate the menu toggle button in the last cell.
         */
        public Locator menuToggle() {
            return buttonWithin(cells().last());
        }

        public Locator cells() {
            return locator.locator("td");
        }

        public Locator cell(String dataLabel) {
            return locator.locator("td" + dataLabel(dataLabel));
        }
    }

    private static Locator buttonWithin(Locator context) {
        return context.locator("button");
    }

    private static String titleOuiaId() {
        return ouiaId("title");
    }

    private static String ouiaId(String id) {
        return attributeSelector("data-ouia-component-id", "=", id);
    }

    private static String dataLabel(String dataLabel) {
        return attributeSelector("data-label", "=", dataLabel);
    }

    private static String classMatcher(String operator, String value) {
        return attributeSelector("class", operator, value);
    }

    private static String attributeSelector(String name, String operator, String value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append('[');
        buffer.append(name);
        buffer.append(operator);
        buffer.append('"');
        buffer.append(value);
        buffer.append('"');
        buffer.append(']');
        return buffer.toString();
    }
}
