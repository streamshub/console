package com.github.streamshub.systemtests.locators;

public class ClusterOverviewPageSelectors {
    private ClusterOverviewPageSelectors() {}
    
    public static final String COPS_RECONCILIATION_PAUSED_NOTIFICATION = new CssBuilder(CssSelectors.PAGE_DIV)
        .withChild()
        .withElementDiv().withComponentPage().withChild()
        .withElementDiv().withComponentPage().withSubComponentMainContainer().withChild()
        .withElementMain().withComponentPage().withSubComponentMain().withChild()
        .withElementDiv().withComponentBanner().withChild()
        .withElementDiv().withLayoutBullseye()
        .build();

    public static final String COPS_RECONCILIATION_PAUSED_NOTIFICATION_RESUME_BUTTON = new CssBuilder(COPS_RECONCILIATION_PAUSED_NOTIFICATION)
        .withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementButton().withComponentButton().withChild()
        .withElementSpan().withComponentButton().withSubComponentText()
        .build();

    public static final String COPS_CLUSTER_CARDS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(1).withChild()
        .withElementDiv().withLayoutFlex()
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_INFO = new CssBuilder(COPS_CLUSTER_CARDS)
        .withChild()
        .withElementDiv().nth(1).withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody()
        .build();

    public static final String COPS_KAFKA_PAUSE_RECONCILIATION_BUTTON = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_INFO)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(1).withChild()
        .withElementDiv().nth(2).withChild()
        .withElementButton().withComponentButton()
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_NAME = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_INFO)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(2).withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withLayoutFlex().nth(1).withChild()
        .withElementDiv().withChild()
        .withElementH2().withComponentTitle()
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_WARNINGS = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_INFO)
        .withChild()
        .withElementDiv().withLayoutFlex().nth(2).withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentExpandableSection()
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_DATA_ITEMS = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_INFO)
        .withElementDiv().withLayoutFlex().nth(2).withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem()
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_DATA_BROKER_COUNT = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(1).withChild()
        .withElementA()
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_DATA_CONSUMER_COUNT = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(2).withChild()
        .withElementA()
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_DATA_KAFKA_VERSION = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_DATA_ITEMS)
        .nth(3).withChild()
        .withElementDiv().nth(1)
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_WARNINGS_DROPDOWN_BUTTON = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_WARNINGS)
        .withElementDiv().withComponentExpandableSection().withSubComponentToggle().withChild()
        .withElementButton()
        .build();

    public static final String COPS_CLUSTER_CARD_KAFKA_WARNING_MESSAGE_ITEMS = new CssBuilder(COPS_CLUSTER_CARD_KAFKA_WARNINGS)
        .withElementDiv().withComponentExpandableSection().withSubComponentContent().withChild()
        .withElementUl().withComponentDataList().withChild()
        .withElementLi().withComponentDataList().withSubComponentItem()
        .build();

    public static final String COPS_TOPIC_COLUMN_CARD_ITEMS = new CssBuilder(CssSelectors.PAGES_CONTENT)
        .withChild()
        .withElementDiv().withComponentPage().withSubComponentMainBody().withChild()
        .withElementDiv().withLayoutGrid().withChild()
        .withElementDiv().withLayoutGrid().withSubComponentItem().nth(2).withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv()
        .build();

    public static final String COPS_RECENT_TOPICS_CARD = new CssBuilder(COPS_TOPIC_COLUMN_CARD_ITEMS)
        .nth(1)
        .withElementDiv().withComponentCard()
        .build();

    public static final String COPS_RECENT_TOPICS_CARD_HEADER = new CssBuilder(COPS_RECENT_TOPICS_CARD)
        .nth(1)
        .withElementDiv().withComponentCard().withSubComponentHeader().withChild()
        .withElementDiv().withComponentCard().withSubComponentHeaderMain().withChild()
        .withElementDiv().withComponentContent()
        .build();

    public static final String COPS_RECENT_TOPICS_CARD_BODY = new CssBuilder(COPS_RECENT_TOPICS_CARD)
        .withChild()
        .withElementDiv().withComponentCard().withSubComponentExpandableContent().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody()
        .build();

    public static final String COPS_RECENT_TOPICS_CARD_TABLE_ITEMS = new CssBuilder(COPS_RECENT_TOPICS_CARD_BODY)
        .withChild()
        .withElementTable().withComponentTable().withChild()
        .withElementTbody().withComponentTable().withSubComponentTbody()
        .build();

    public static final String COPS_TOPICS_CARD = new CssBuilder(COPS_TOPIC_COLUMN_CARD_ITEMS)
        .nth(2)
        .withElementDiv().withComponentCard()
        .build();

    public static final String COPS_TOPICS_CARD_TOP_BODY_ITEMS = new CssBuilder(COPS_TOPICS_CARD)
        .withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv()
        .build();

    public static final String COPS_TOPICS_CARD_TOTAL_TOPICS = new CssBuilder(COPS_TOPICS_CARD_TOP_BODY_ITEMS)
        .nth(1)
        .build();

    public static final String COPS_TOPICS_CARD_TOTAL_PARTITIONS = new CssBuilder(COPS_TOPICS_CARD_TOP_BODY_ITEMS)
        .nth(2)
        .build();

    public static final String COPS_TOPICS_CARD_BODY_ITEMS = new CssBuilder(COPS_TOPICS_CARD)
        .withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withLayoutFlex().nth(2).withChild()
        .withElementDiv()
        .build();

    public static final String COPS_TOPICS_CARD_FULLY_REPLICATED = new CssBuilder(COPS_TOPICS_CARD_BODY_ITEMS)
        .nth(1)
        .build();

    public static final String COPS_TOPICS_CARD_UNDER_REPLICATED = new CssBuilder(COPS_TOPICS_CARD_BODY_ITEMS)
        .nth(2)
        .build();

    public static final String COPS_TOPICS_CARD_UNAVAILABLE = new CssBuilder(COPS_TOPICS_CARD_BODY_ITEMS)
        .nth(3)
        .build();

    public static final String COPS_DISK_SPACE_CHART_NODE_TEXT_ITEMS = new CssBuilder(COPS_CLUSTER_CARDS)
        .withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentChart().withChild()
        .withElementSvg().withChild()
        .withElementG().nth(4).withChild()
        .withElementText()
        .build();

    public static final String COPS_CPU_USAGE_CHART_NODE_TEXT_ITEMS = new CssBuilder(COPS_CLUSTER_CARDS)
        .withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(4).withChild()
        .withElementDiv().withComponentChart().withChild()
        .withElementSvg().withChild()
        .withElementG().nth(4).withChild()
        .withElementText()
        .build();

    public static final String COPS_MEMORY_USAGE_CHART_NODE_TEXT_ITEMS = new CssBuilder(COPS_CLUSTER_CARDS)
        .withChild()
        .withElementDiv().nth(2).withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().nth(6).withChild()
        .withElementDiv().withComponentChart().withChild()
        .withElementSvg().withChild()
        .withElementG().nth(4).withChild()
        .withElementText()
        .build();

    public static final String COPS_TOPIC_BYTES_CHART_NODE_TEXT_ITEMS = new CssBuilder(COPS_TOPIC_COLUMN_CARD_ITEMS)
        .withChild()
        .withElementDiv().withComponentCard().withChild()
        .withElementDiv().withComponentCard().withSubComponentBody().withChild()
        .withElementDiv().withLayoutFlex().withChild()
        .withElementDiv().withChild()
        .withElementDiv().withComponentChart().withChild()
        .withElementSvg().withChild()
        .withElementG().nth(4).withChild()
        .withElementText()
        .build();
}
