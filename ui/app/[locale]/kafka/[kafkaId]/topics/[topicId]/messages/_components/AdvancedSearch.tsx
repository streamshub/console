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
  filterLimit,
  onSearch,
}: Pick<
  MessageBrowserProps,
  | "filterQuery"
  | "filterEpoch"
  | "filterOffset"
  | "filterPartition"
  | "filterTimestamp"
  | "filterLimit"
  | "onSearch"
>) {
  const searchInputRef = useRef<HTMLInputElement>(null);
  const paneRef = useRef(null);
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [query, setQuery] = useState(filterQuery);
  const [fromEpoch, setFromEpoch] = useState(filterEpoch);
  const [fromTimestamp, setFromTimestamp] = useState(filterTimestamp);
  const [fromOffset, setFromOffset] = useState(filterOffset);
  const [untilLimit, setUntilLimit] = useState(filterLimit);
  const [shouldSubmit, setShouldSubmit] = useState(false);
  const [_, startTransition] = useTransition();

  function setLatest() {
    setFromEpoch(undefined);
    setFromTimestamp(undefined);
    setFromOffset(undefined);
  }

  function onClear() {
    setQuery(undefined);
    setLatest();
    setUntilLimit(50);
    setShouldSubmit(true);
  }

  function onSubmit(
    e?: MouseEvent<HTMLButtonElement> | SyntheticEvent<HTMLButtonElement>,
  ) {
    e && e.preventDefault();
    setShouldSubmit(true);
  }

  const getParameters = useCallback((): SearchParams => {
    const from = ((): SearchParams["from"] => {
      if (fromOffset) {
        return { type: "offset", value: fromOffset };
      } else if (fromEpoch) {
        return { type: "epoch", value: fromEpoch };
      } else if (fromTimestamp) {
        return { type: "timestamp", value: fromTimestamp };
      } else {
        return { type: "latest" };
      }
    })();
    return {
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
        value: untilLimit,
      },
    };
  }, [query, untilLimit, fromOffset, fromEpoch, fromTimestamp]);

  const getSearchInputValue = useCallback(() => {
    const parameters = getParameters();
    const { query, from, until, partition } = parameters;
    return (() => {
      let composed: string[] = [];
      if (query) {
        composed.push(query.value);
        if (query.where.type !== "everywhere") {
          composed.push(`where=${query.where.type}`);
        }
      }
      if (from) {
        if ("value" in from) {
          composed.push(`from=${from.type}:${from.value}`);
        } else {
          composed.push(`from=now`);
        }
      }
      if (until) {
        if ("value" in until) {
          composed.push(`until=${until.type}:${until.value}`);
        }
      }
      return composed.join(" ");
    })();
  }, [getParameters]);

  const [searchInputValue, setSearchInputValue] = useState(
    getSearchInputValue(),
  );

  const doSubmit = useCallback(
    (e?: MouseEvent<HTMLButtonElement> | SyntheticEvent<HTMLButtonElement>) => {
      e && e.preventDefault();
      startTransition(() => {
        onSearch(getParameters());
      });
    },
    [getParameters, onSearch],
  );

  function onComposed(value: string) {
    const parts = value.split(" ");
    let queryParts: string[] = [];
    parts.forEach((p) => {
      if (p.indexOf(`where=`) === 0) {
        const [_, where] = p.split("=");
        // TODO set where
      } else if (p.indexOf(`from=`) === 0) {
        const [_, from] = p.split("=");
        if (from === "now") {
          setLatest();
        } else {
          const [type, value] = from.split(":");
          switch (type) {
            case "offset": {
              const number = parseInt(value, 10);
              if (Number.isSafeInteger(number)) {
                setFromOffset(number);
              } else {
                setLatest();
              }
              break;
            }
            case "epoch": {
              const number = parseInt(value, 10);
              if (Number.isSafeInteger(number)) {
                setFromEpoch(number);
              } else {
                setLatest();
              }
              break;
            }
            case "timestamp": {
              setFromTimestamp(value);
              break;
            }
            default:
              setLatest();
          }
        }
      } else if (p.indexOf("until=") === 0) {
        const [_, until] = p.split("=");
        const [type, value] = until.split(":");
        switch (type) {
          case "limit": {
            const number = parseInt(value, 10);
            if (Number.isSafeInteger(number)) {
              setUntilLimit(number);
            }
            break;
          }
          case "partition": {
            // const number = parseInt(value, 10);
            // if (Number.isSafeInteger(number)) {
            //   setEpoch(number);
            // } else {
            //   setLatest();
            // }
            break;
          }
          case "timestamp": {
            // TODO
            break;
          }
          default:
            setLatest();
        }
      } else {
        queryParts.push(p);
      }
    });
    setQuery(queryParts.join(" "));
  }

  useEffect(() => {
    if (shouldSubmit) {
      setShouldSubmit(false);
      setIsPanelOpen(false);
      void doSubmit();
    }
  }, [doSubmit, shouldSubmit]);

  const searchInput = (
    <SearchInput
      value={searchInputValue}
      onChange={(_, v) => setSearchInputValue(v)}
      onToggleAdvancedSearch={(e) => {
        e.stopPropagation();
        setIsPanelOpen((v) => !v);
      }}
      isAdvancedSearchOpen={isPanelOpen}
      onClear={onClear}
      onSearch={(e) => {
        onComposed(searchInputValue);
        onSubmit(e);
      }}
      ref={searchInputRef}
      id="search"
    />
  );

  const advancedForm = (
    <div ref={paneRef} role="dialog" aria-label="Search messages">
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
                  onOffsetChange={setFromOffset}
                  onTimestampChange={setFromTimestamp}
                  onEpochChange={setFromEpoch}
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
                      {untilLimit}
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
      appendTo={() => document.querySelector("body")!}
    />
  );
}
