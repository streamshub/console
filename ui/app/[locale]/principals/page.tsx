import {
  Caption,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@/libs/patternfly/react-table";

async function getData() {
  const res = await fetch("https://swapi.dev/api/people");
  // The return value is *not* serialized
  // You can return Date, Map, Set, etc.

  if (!res.ok) {
    // This will activate the closest `error.js` Error Boundary
    throw new Error("Failed to fetch data");
  }

  return res.json();
}

export default async function Principals() {
  const data = await getData();

  return (
    <Table aria-label="Simple table">
      <Caption>Simple table using composable components</Caption>
      <Thead>
        <Tr>
          <Th>Name</Th>
          <Th>Height</Th>
          <Th>Mass</Th>
          <Th>Hair_color</Th>
          <Th>Skin_color</Th>
          <Th>Eye_color</Th>
          <Th>Birth_year</Th>
          <Th>Gender</Th>
        </Tr>
      </Thead>
      <Tbody>
        {data.results.map((p) => (
          <Tr key={p.name}>
            <Td dataLabel={"Name"}>{p.name}</Td>
            <Td dataLabel={"Height"}>{p.height}</Td>
            <Td dataLabel={"Mass"}>{p.mass}</Td>
            <Td dataLabel={"Hair_color"}>{p.hair_color}</Td>
            <Td dataLabel={"Skin_color"}>{p.skin_color}</Td>
            <Td dataLabel={"Eye_color"}>{p.eye_color}</Td>
            <Td dataLabel={"Birth_year"}>{p.birth_year}</Td>
            <Td dataLabel={"Gender"}>{p.gender}</Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  );
}
