#!/usr/bin/env python3
"""Compose already validated fragments into a controlled offline stationery preview."""
import argparse
import hashlib
import html
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET


def build(source: Path, rendered: Path, output: Path) -> None:
    source = source.resolve()
    rendered = rendered.resolve()
    output = output.resolve()
    if output.is_relative_to(source):
        raise ValueError("Output must be outside stationery source")
    manifest = json.loads((rendered / "rendered-manifest.json").read_text())
    if manifest["scope"] != "SANDBOX_DESIGN_PREVIEW":
        raise ValueError("Only explicit sandbox design fragments are supported")
    assets = json.loads((source / "preview/asset-manifest.json").read_text())["assets"]
    for name, expected in assets.items():
        data = (source / "preview/assets" / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != expected["sha256"]:
            raise ValueError("Changed controlled preview asset: " + name)
    output.mkdir(parents=True, exist_ok=True)
    shutil.copytree(source / "preview/assets", output / "assets", dirs_exist_ok=True)
    shutil.copy2(source / "preview/profile.css", output / "profile.css")
    pages = []
    for entry in manifest["entries"]:
        fragment_path = (rendered / entry["fragment"]).resolve()
        if not fragment_path.is_relative_to(rendered):
            raise ValueError("Fragment path escapes rendered output")
        raw = fragment_path.read_bytes()
        if hashlib.sha256(raw).hexdigest() != entry["sha256"]:
            raise ValueError("Fragment digest mismatch")
        tree = ET.fromstring("<preview>" + raw.decode() + "</preview>")
        sections = list(tree)
        if not sections or any(s.tag != "section" or s.attrib != {"class": "document-page"} for s in sections):
            raise ValueError("Expected explicitly paged validated sections")
        sheets = []
        for number, section in enumerate(sections, 1):
            logo = "firefly-primary.svg" if number == 1 else "firefly-compact.svg"
            classes = "sheet" + (" sheet--continuation" if number > 1 else "")
            if entry["code"] == "SBX_BANK_ACCOUNT_STATEMENT":
                classes += " sheet--statement"
            elif entry["code"] == "SBX_INSURANCE_PREMIUM_INVOICE":
                classes += " sheet--invoice"
            header = ('<header class="document-header"><img class="document-logo" src="assets/' + logo
                      + '" alt="Firefly" /><div class="issuer-details"><strong>Firefly Sandbox Financial Services</strong>'
                      + '<span>Fictional demonstration entity</span><span>100 Example Avenue · Example City</span>'
                      + '<span>support@example.invalid</span></div></header>')
            footer = ('<footer class="document-footer"><div>Firefly Sandbox Financial Services · Demonstration entity DEMO-FIN-001</div>'
                      + '<div class="document-footer__row"><span>' + html.escape(entry["code"] + " / " + entry["version"])
                      + ' · ' + html.escape(entry["documentReference"]) + '</span><span class="document-footer__page">Page '
                      + str(number) + ' of ' + str(len(sections)) + '</span></div></footer>')
            sheets.append('<div class="' + classes + '">' + header + '<main class="document-content">'
                          + ET.tostring(section, encoding="unicode", method="html") + '</main>' + footer + '</div>')
        filename = entry["code"].lower() + ".html"
        document = ('<!doctype html><html lang="en-US"><head><meta charset="utf-8" />'
                    + '<meta name="viewport" content="width=device-width, initial-scale=1" />'
                    + '<title>' + html.escape(entry["name"]) + ' — Firefly sandbox specimen</title>'
                    + '<link rel="stylesheet" href="profile.css" /></head><body>' + ''.join(sheets) + '</body></html>')
        (output / filename).write_text(document)
        pages.append({"file": filename, "name": entry["name"], "pages": len(sections),
                      "localeCode": entry["localeCode"], "currency": entry["currency"],
                      "fragment_sha256": entry["sha256"], "html_sha256": hashlib.sha256(document.encode()).hexdigest()})
    (output / "preview-manifest.json").write_text(json.dumps({"scope": "SANDBOX_DESIGN_PREVIEW_NOT_ISSUED", "documents": pages}, indent=2) + "\n")
    cards = []
    for entry in pages:
        stem = Path(entry["file"]).stem
        image = ('<img src="' + stem + '.png" alt="First page of ' + html.escape(entry["name"])
                 + '" loading="lazy" />') if (output / (stem + '.png')).is_file() else ''
        pdf = ('<a href="' + stem + '.pdf">Print preview PDF</a>') if (output / (stem + '.pdf')).is_file() else ''
        cards.append('<article class="preview-card"><a class="preview-card__image" href="' + entry["file"]
                     + '">' + image + '</a><div class="preview-card__body"><p class="preview-kicker">'
                     + str(entry["pages"]) + ' page' + ('s' if entry["pages"] != 1 else '')
                     + ' · ' + html.escape(entry["localeCode"])
                     + (' · ' + html.escape(entry["currency"]) if entry["currency"] else '')
                     + '</p><h2>' + html.escape(entry["name"]) + '</h2><nav aria-label="'
                     + html.escape(entry["name"]) + ' formats"><a href="' + entry["file"]
                     + '">Responsive HTML</a>' + pdf + '</nav></div></article>')
    (output / "index.html").write_text('<!doctype html><html lang="en-US"><head><meta charset="utf-8" />'
        + '<meta name="viewport" content="width=device-width, initial-scale=1" /><title>Firefly sandbox stationery</title>'
        + '<link rel="stylesheet" href="profile.css" /></head><body class="preview-gallery"><header class="preview-gallery__header">'
        + '<img class="document-logo" src="assets/firefly-primary.svg" alt="Firefly" /><p class="preview-kicker">OpenPlatform / Document design</p>'
        + '<h1>Financial stationery,<br />designed to be read.</h1><p>Official Firefly identity, clear financial tables and carefully composed pages for banking, lending and insurance.</p>'
        + '<p class="preview-notice">Sandbox design collection. Fictional institution and customer data. These previews are not issued customer documents or an available PDF-generation API.</p>'
        + '</header><main class="preview-grid">' + ''.join(cards) + '</main></body></html>')
    print(json.dumps({"documents": len(pages), "pages": sum(p["pages"] for p in pages), "output": str(output)}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--rendered", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    build(args.source, args.rendered, args.output)
