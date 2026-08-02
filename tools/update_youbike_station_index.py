#!/usr/bin/env python3
"""Generate the bundled YouBike station/region index from TDX.

Set TDX_CLIENT_ID and TDX_CLIENT_SECRET in the environment. Credentials and raw
responses are never written to the repository.
"""

import json
import os
import pathlib
import time
import urllib.parse
import urllib.request
from urllib.error import HTTPError
from datetime import datetime, timezone

TOKEN_URL = "https://tdx.transportdata.tw/auth/realms/TDXConnect/protocol/openid-connect/token"
API_ROOT = "https://tdx.transportdata.tw/api/basic/v2/Bike/Station/City"
CITIES = {
    "Taipei": "Taipei",
    "NewTaipei": "NewTaipei",
    "Taoyuan": "Taoyuan",
    "Hsinchu": None,
    "HsinchuCounty": "HsinchuCounty",
    "MiaoliCounty": "Miaoli",
    "Taichung": "Taichung",
    "Chiayi": "ChiayiCity",
    "ChiayiCounty": "ChiayiCounty",
    "Tainan": "Tainan",
    "Kaohsiung": "Kaohsiung",
    "PingtungCounty": "Pingtung",
    "TaitungCounty": "Taitung",
}
OUTPUT = pathlib.Path(__file__).parents[1] / "app/src/main/res/raw/youbike_stations.tsv"
LOCAL_PROPERTIES = pathlib.Path(__file__).parents[1] / "local.properties"


def read_local_properties():
    if not LOCAL_PROPERTIES.exists():
        return {}
    properties = {}
    for raw_line in LOCAL_PROPERTIES.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def request_json(url, data=None, headers=None):
    for attempt in range(5):
        request = urllib.request.Request(url, data=data, headers=headers or {})
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return json.load(response)
        except HTTPError as error:
            if error.code != 429 or attempt == 4:
                raise
            retry_after = error.headers.get("Retry-After")
            delay = float(retry_after) if retry_after else 2 ** (attempt + 1)
            time.sleep(max(delay, 1))
    raise RuntimeError("TDX request retry limit reached")


def station_region(city, default_region, station):
    if city != "Hsinchu":
        return default_region
    # TDX groups Hsinchu City and Hsinchu Science Park under the Hsinchu
    # endpoint. Science Park station IDs use the official 5082 prefix.
    station_id = str(station.get("StationID", ""))
    return "HsinchuSciencePark" if station_id.startswith("5082") else "HsinchuCity"


def main():
    local_properties = read_local_properties()
    client_id = os.environ.get("TDX_CLIENT_ID") or local_properties.get("TDX_CLIENT_ID")
    client_secret = os.environ.get("TDX_CLIENT_SECRET") or local_properties.get("TDX_CLIENT_SECRET")
    if not client_id or not client_secret:
        raise SystemExit(
            "Set TDX_CLIENT_ID and TDX_CLIENT_SECRET in the environment or local.properties "
            "before running this tool."
        )
    token = request_json(
        TOKEN_URL,
        urllib.parse.urlencode({
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
        }).encode(),
        {"content-type": "application/x-www-form-urlencoded"},
    )["access_token"]
    rows = set()
    for city, region in CITIES.items():
        stations = request_json(
            f"{API_ROOT}/{city}?%24format=JSON",
            headers={"authorization": f"Bearer {token}"},
        )
        for station in stations:
            name = station.get("StationName", {}).get("Zh_tw", "").strip()
            if name.startswith("YouBike2.0_"):
                rows.add((name, station_region(city, region, station)))
        time.sleep(0.25)
    generated_at = datetime.now(timezone.utc).isoformat(timespec="seconds")
    content = [
        "# Generated station-name index. Refresh with tools/update_youbike_station_index.py.",
        f"# generatedAt={generated_at} source=TDX rows={len(rows)}",
    ]
    content.extend(f"{name}\t{region}" for name, region in sorted(rows))
    OUTPUT.write_text("\n".join(content) + "\n", encoding="utf-8")
    print(f"Wrote {len(rows)} station/region rows to {OUTPUT}")


if __name__ == "__main__":
    main()
