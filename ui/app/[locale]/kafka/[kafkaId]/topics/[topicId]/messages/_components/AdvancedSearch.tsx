import { FilterGroup } from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/FilterGroup";
import {
  MessageBrowserProps,
  SearchParams,
} from "@/app/[locale]/kafka/[kafkaId]/topics/[topicId]/messages/_components/MessagesTable";
import {
  ActionGroup,
  Button,
  Dropdown,
  DropdownItem,
  Form,
  FormGroup,
  FormHelperText,
  Grid,
  GridItem,
  HelperText,
  HelperTextItem,
  MenuToggle,
  Panel,
  PanelMain,
  PanelMainBody,
  Popper,
  SearchInput,
  TextInput,
} from "@patternfly/react-core";
import {
  MouseEvent,
  SyntheticEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
  useTransition,
} from "react";

export function AdvancedSearch({
  filterQuery,
  filterEpoch,
  filterOffset,
  filterPartition,
  filterTimestamp,
  onSearch,
}: Pick<
  MessageBrowserProps,
  | "filterQuery"
  | "filterEpoch"
  | "filterOffset"
  | "filterPartition"
  | "filterTimestamp"
  | "onSearch"
>) {
  const searchInputRef = useRef<HTMLInputElement>(null);
  const paneRef = useRef(null);
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [query, setQuery] = useState(filterQuery);
  const [epoch, setEpoch] = useState(filterEpoch);
  const [timestamp, setTimestamp] = useState(filterTimestamp);
  const [offset, setOffset] = useState(filterOffset);
  const [sholdSubmit, setShouldSubmit] = useState(false);
  const [_, startTransition] = useTransition();

  function setLatest() {
    setEpoch(undefined);
    setTimestamp(undefined);
    setOffset(undefined);
  }

  function onClear() {
    setQuery(undefined);
    setLatest();
    setShouldSubmit(true);
  }

  function onSubmit(
    e?: MouseEvent<HTMLButtonElement> | SyntheticEvent<HTMLButtonElement>,
  ) {
    e && e.preventDefault();
    setShouldSubmit(true);
  }

  const doSubmit = useCallback(
    (e?: MouseEvent<HTMLButtonElement> | SyntheticEvent<HTMLButtonElement>) => {
      e && e.preventDefault();
      startTransition(() => {
        const from = ((): SearchParams["from"] => {
          if (offset) {
            return { type: "offset", value: offset };
          } else if (epoch) {
            return { type: "epoch", value: epoch };
          } else if (timestamp) {
            return { type: "timestamp", value: timestamp };
          } else {
            return { type: "latest" };
          }
        })();
        onSearch({
          query: query
            ? {
                value: query,
                where: {
                  type: "everywhere",
                },
              }
            : undefined,
          from,
          until: {
            type: "limit",
            value: 50,
          },
        });
      });
    },
    [epoch, offset, onSearch, query, timestamp],
  );

  useEffect(() => {
    if (sholdSubmit) {
      setShouldSubmit(false);
      setIsPanelOpen(false);
      void doSubmit();
    }
  }, [doSubmit, sholdSubmit]);

  const searchInput = (
    <SearchInput
      value={query}
      onChange={(_, v) => {
        setQuery(v);
      }}
      onToggleAdvancedSearch={(e) => {
        e.stopPropagation();
        setIsPanelOpen((v) => !v);
      }}
      isAdvancedSearchOpen={isPanelOpen}
      onClear={onClear}
      onSearch={onSubmit}
      ref={searchInputRef}
      id="search"
    />
  );

  const advancedForm = (
    <div ref={paneRef} role="dialog" aria-label="Advanced search form">
      <Panel variant="raised">
        <PanelMain>
          <PanelMainBody>
            <Form>
              <Grid hasGutter={true}>
                <GridItem span={6}>
                  <FormGroup
                    label="Has the words"
                    fieldId="has-words"
                    key="has-words"
                  >
                    <TextInput
                      type="text"
                      id="query"
                      value={query}
                      onChange={(_event, value) => setQuery(value)}
                    />
                  </FormGroup>
                </GridItem>

                <GridItem span={6}>
                  <FormGroup label={"Where"}>
                    <Dropdown
                      data-testid={"filter-group-dropdown"}
                      toggle={(toggleRef) => (
                        <MenuToggle
                          onClick={() => {}}
                          isDisabled={false}
                          isExpanded={false}
                          data-testid={"filter-group"}
                          ref={toggleRef}
                        >
                          Anywhere
                        </MenuToggle>
                      )}
                      isOpen={false}
                      onOpenChange={() => {}}
                      onSelect={() => {}}
                    >
                      <DropdownItem key="offset" value="offset">
                        Anywhere
                      </DropdownItem>
                    </Dropdown>
                  </FormGroup>
                </GridItem>
              </Grid>
              <FormGroup label={"Search from"}>
                <FilterGroup
                  isDisabled={false}
                  offset={filterOffset}
                  epoch={filterEpoch}
                  timestamp={filterTimestamp}
                  onOffsetChange={setOffset}
                  onTimestampChange={setTimestamp}
                  onEpochChange={setEpoch}
                  onLatest={setLatest}
                />
              </FormGroup>
              <FormGroup label={"Until"}>
                <Dropdown
                  data-testid={"filter-group-dropdown"}
                  toggle={(toggleRef) => (
                    <MenuToggle
                      onClick={() => {}}
                      isDisabled={false}
                      isExpanded={false}
                      data-testid={"filter-group"}
                      ref={toggleRef}
                    >
                      Number of messages
                    </MenuToggle>
                  )}
                  isOpen={false}
                  onOpenChange={() => {}}
                  onSelect={() => {}}
                >
                  <DropdownItem key="offset" value="offset">
                    Now
                  </DropdownItem>
                </Dropdown>
                <Dropdown
                  data-testid={"filter-group-dropdown"}
                  toggle={(toggleRef) => (
                    <MenuToggle
                      onClick={() => {}}
                      isDisabled={false}
                      isExpanded={false}
                      data-testid={"filter-group"}
                      ref={toggleRef}
                    >
                      50
                    </MenuToggle>
                  )}
                  isOpen={false}
                  onOpenChange={() => {}}
                  onSelect={() => {}}
                >
                  <DropdownItem key="offset" value="offset">
                    Now
                  </DropdownItem>
                </Dropdown>
              </FormGroup>
              <FormGroup
                label="Value transformation"
                fieldId="has-words"
                key="has-words"
              >
                <TextInput type="text" id="has-words" />
                <FormHelperText>
                  <HelperText>
                    <HelperTextItem>
                      Tranform the value using a jq filter. This is useful when
                      the value contains many fields and you want a quick glance
                      at some of them.
                    </HelperTextItem>
                  </HelperText>
                </FormHelperText>
              </FormGroup>
              <ActionGroup>
                <Button
                  variant="primary"
                  type="submit"
                  onClick={(e) => onSubmit(e)}
                >
                  Search
                </Button>
                {!!onClear && (
                  <Button variant="link" type="reset" onClick={onClear}>
                    Reset
                  </Button>
                )}
              </ActionGroup>
            </Form>
          </PanelMainBody>
        </PanelMain>
      </Panel>
    </div>
  );

  // Popper is just one way to build a relationship between a toggle and a menu.
  return (
    <Popper
      trigger={searchInput}
      triggerRef={searchInputRef}
      popper={advancedForm}
      popperRef={paneRef}
      isVisible={isPanelOpen}
      enableFlip={false}
      appendTo={() => document.querySelector("#search")!}
    />
  );
}
