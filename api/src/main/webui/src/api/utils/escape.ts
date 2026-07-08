export default function escape(value: string) {
  return value.replace(/\\/g, '\\\\').replace(/,/g, '\\,');
}
