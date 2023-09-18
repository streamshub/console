const K = 1024;
const UNITS = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];;

export function formatBytes(bytes: number | null, decimals: number) {
    if (bytes == null) {
        return '-';
    }

    if (bytes == 0) {
        return '0 B';
    }

    let dm = decimals || 2;
    let i = Math.floor(Math.log(bytes) / Math.log(K));

    return parseFloat((bytes / Math.pow(K, i)).toFixed(dm)) + ' ' + UNITS[i];
}
