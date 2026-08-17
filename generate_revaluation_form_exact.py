"""
EXACT Revaluation Claim Form Generator — K.RAMAKRISHNAN COLLEGE OF ENGINEERING
Pixel-perfect A4 Landscape recreation matching the official scanned sample.

Layout:
  +------------------------------------------------------------------+
  | [LOGO]  Office of the Controller of Examinations                  |
  |         K.RAMAKRISHNAN COLLEGE OF ENGINEERING (Autonomous)        |
  |         ESE - Re-Valuation - DEC 2025 Examinations               |
  +------------------------------------------------------------------+
  | Post Held EXAMINER | Date of Valuation | Name | Mobile | Desig.  |
  | Name of Institution |                        | Board             |
  +------------------------------------------------------------------+
  | Detail of Subjects Valued     | Detail of Claim Amount           |
  | FN: S.N.|SubCode|Scripts      |                                  |
  | AN: S.N.|SubCode|Scripts      | Script Amt / TA / DA / Total    |
  |                               | Received Rs. xxx/-              |
  |                               | Signature of the Examiner       |
  +------------------------------------------------------------------+
  | ACCOUNT NUMBER: | IFSC CODE:  | Signature of Chief Examiner     |
  | BANK NAME:      | BRANCH:     |                                  |
  +------------------------------------------------------------------+
"""

import os
from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_ORIENT
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml.ns import nsdecls
from docx.oxml import parse_xml

NUM_ROWS = 15  # User requested 15 rows in FN and AN

# ──── Helper Functions ────────────────────────────────────────────────

def set_cell_border(cell, **kwargs):
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(f'<w:tcBorders {nsdecls("w")}></w:tcBorders>')
    for edge, attrs in kwargs.items():
        element = parse_xml(
            f'<w:{edge} {nsdecls("w")} w:val="{attrs.get("val","single")}" '
            f'w:sz="{attrs.get("sz",4)}" w:space="0" w:color="{attrs.get("color","000000")}"/>')
        tcBorders.append(element)
    tcPr.append(tcBorders)

def all_borders(cell, sz=4):
    set_cell_border(cell,
        top={"sz":sz,"val":"single"}, bottom={"sz":sz,"val":"single"},
        start={"sz":sz,"val":"single"}, end={"sz":sz,"val":"single"})

def no_borders(cell):
    set_cell_border(cell,
        top={"sz":0,"val":"none"}, bottom={"sz":0,"val":"none"},
        start={"sz":0,"val":"none"}, end={"sz":0,"val":"none"})

def set_row_height(row, cm):
    trPr = row._tr.get_or_add_trPr()
    trPr.append(parse_xml(f'<w:trHeight {nsdecls("w")} w:val="{int(cm*567)}" w:hRule="atLeast"/>'))

def set_cell_width(cell, cm):
    cell.width = Cm(cm)

def fmt(cell, text, bold=False, italic=False, sz=9, align=WD_ALIGN_PARAGRAPH.CENTER, font='Times New Roman'):
    """Write text into a cell, supporting \\n for multi-line."""
    cell.text = ""
    lines = str(text).split('\n') if text else [""]
    for i, line in enumerate(lines):
        p = cell.paragraphs[0] if i == 0 else cell.add_paragraph()
        p.alignment = align
        pf = p.paragraph_format
        pf.space_before = Pt(0)
        pf.space_after = Pt(0)
        pf.line_spacing = Pt(sz + 2)
        run = p.add_run(line)
        run.font.size = Pt(sz)
        run.font.bold = bold
        run.font.italic = italic
        run.font.name = font
    cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER

def add_para(container, text, sz=9, bold=False, italic=False, align=WD_ALIGN_PARAGRAPH.CENTER,
             underline=False, font='Times New Roman', space_before=0, space_after=0):
    """Add a paragraph to a document or cell."""
    p = container.add_paragraph() if hasattr(container, 'add_paragraph') else container.paragraphs[0]
    if not hasattr(container, 'add_paragraph'):
        p = container.add_paragraph()
    p.alignment = align
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.space_after = Pt(space_after)
    run = p.add_run(text)
    run.font.size = Pt(sz)
    run.font.bold = bold
    run.font.italic = italic
    run.font.name = font
    if underline:
        run.font.underline = True
    return p

def vmerge_restart(cell):
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:vMerge {nsdecls("w")} w:val="restart"/>'))

def vmerge_continue(cell):
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:vMerge {nsdecls("w")} w:val="continue"/>'))

# ──── Main Generator ──────────────────────────────────────────────────

def generate(output_path):
    doc = Document()

    # ── Page Setup: A4 Landscape ──
    sec = doc.sections[0]
    sec.orientation = WD_ORIENT.LANDSCAPE
    sec.page_width = Cm(29.7)
    sec.page_height = Cm(21.0)
    sec.top_margin = Cm(0.6)
    sec.bottom_margin = Cm(0.5)
    sec.left_margin = Cm(0.8)
    sec.right_margin = Cm(0.8)

    FONT = 'Times New Roman'

    # ══════════════════════════════════════════════════════════════════════
    # 1. HEADER: Logo + Title (borderless 1×2 table)
    # ══════════════════════════════════════════════════════════════════════
    hdr = doc.add_table(rows=1, cols=2)
    hdr.alignment = WD_TABLE_ALIGNMENT.CENTER
    for c in hdr.rows[0].cells: no_borders(c)

    # Left cell: Logo
    logo_cell = hdr.rows[0].cells[0]
    logo_cell.width = Cm(2.2)
    logo_path = os.path.join(os.path.dirname(output_path), "src", "main", "resources", "logo1.png")
    p_logo = logo_cell.paragraphs[0]
    p_logo.alignment = WD_ALIGN_PARAGRAPH.CENTER
    if os.path.exists(logo_path):
        try:
            p_logo.add_run().add_picture(logo_path, width=Cm(1.6))
        except Exception:
            pass

    # Right cell: Title text
    title_cell = hdr.rows[0].cells[1]
    title_cell.width = Cm(25.7)

    p0 = title_cell.paragraphs[0]
    p0.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p0.paragraph_format.space_after = Pt(0)
    r = p0.add_run("Office of the Controller of Examinations")
    r.font.size = Pt(16); r.font.bold = True; r.font.name = FONT

    p1 = title_cell.add_paragraph()
    p1.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p1.paragraph_format.space_before = Pt(1); p1.paragraph_format.space_after = Pt(0)
    r = p1.add_run("K.RAMAKRISHNAN COLLEGE OF ENGINEERING")
    r.font.size = Pt(14); r.font.bold = True; r.font.name = FONT

    p2 = title_cell.add_paragraph()
    p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p2.paragraph_format.space_before = Pt(0); p2.paragraph_format.space_after = Pt(0)
    r = p2.add_run("(Autonomous)")
    r.font.size = Pt(10); r.font.name = FONT

    p3 = title_cell.add_paragraph()
    p3.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p3.paragraph_format.space_before = Pt(2); p3.paragraph_format.space_after = Pt(2)
    r = p3.add_run("ESE - Re-Valuation - DEC 2025 Examinations")
    r.font.size = Pt(11); r.font.bold = True; r.font.name = FONT; r.font.underline = True

    p4 = title_cell.add_paragraph()
    p4.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p4.paragraph_format.space_before = Pt(1); p4.paragraph_format.space_after = Pt(2)
    r = p4.add_run("No. of Sessions : Only AN")
    r.font.size = Pt(9); r.font.bold = True; r.font.name = FONT

    # ══════════════════════════════════════════════════════════════════════
    # 2. PERSONAL DETAILS TABLE (bordered, 3 rows × 8 cols)
    # ══════════════════════════════════════════════════════════════════════
    info = doc.add_table(rows=3, cols=8)
    info.alignment = WD_TABLE_ALIGNMENT.CENTER
    for row in info.rows:
        for cell in row.cells:
            all_borders(cell)

    # Row 0: Post Held | EXAMINER | (merge 2-5) Date of Valuation
    fmt(info.rows[0].cells[0], "Post Held", bold=True, sz=9)
    fmt(info.rows[0].cells[1], "EXAMINER", bold=True, sz=9)
    info.rows[0].cells[2].merge(info.rows[0].cells[5])
    fmt(info.rows[0].cells[2], "Date of Valuation(dd.mm.yyyy) :", bold=True, sz=9,
        align=WD_ALIGN_PARAGRAPH.LEFT)
    info.rows[0].cells[6].merge(info.rows[0].cells[7])
    fmt(info.rows[0].cells[6], "", sz=9)

    # Row 1: Name | (blank) | Mobile Number | (blank) | Designation | PROFESSOR
    fmt(info.rows[1].cells[0], "Name", bold=True, sz=9)
    info.rows[1].cells[1].merge(info.rows[1].cells[2])
    fmt(info.rows[1].cells[1], "", sz=9)
    fmt(info.rows[1].cells[3], "Mobile Number", bold=True, sz=8)
    info.rows[1].cells[4].merge(info.rows[1].cells[5])
    fmt(info.rows[1].cells[4], "", sz=9)
    fmt(info.rows[1].cells[6], "Designation", bold=True, sz=8)
    fmt(info.rows[1].cells[7], "PROFESSOR", bold=True, sz=9)

    # Row 2: Name of Institution | (merge 1-5 blank) | Board | (blank)
    fmt(info.rows[2].cells[0], "Name of Institution", bold=True, sz=8)
    info.rows[2].cells[1].merge(info.rows[2].cells[5])
    fmt(info.rows[2].cells[1], "", sz=9)
    fmt(info.rows[2].cells[6], "Board", bold=True, sz=9)
    fmt(info.rows[2].cells[7], "", sz=9)

    # Row heights
    for row in info.rows:
        set_row_height(row, 0.55)

    # ══════════════════════════════════════════════════════════════════════
    # 3. MAIN BODY: Parent bordered table (2 rows × 2 cols)
    #    Row 0: LEFT = Subjects | RIGHT = Claim Amount
    #    Row 1: LEFT = (empty) | RIGHT = Signature
    # ══════════════════════════════════════════════════════════════════════
    parent = doc.add_table(rows=2, cols=2)
    parent.alignment = WD_TABLE_ALIGNMENT.CENTER
    for row in parent.rows:
        for cell in row.cells:
            all_borders(cell)

    left = parent.rows[0].cells[0]
    right = parent.rows[0].cells[1]
    left_sig = parent.rows[1].cells[0]
    right_sig = parent.rows[1].cells[1]
    left.width = Cm(14.5)
    right.width = Cm(13.3)
    left_sig.width = Cm(14.5)
    right_sig.width = Cm(13.3)

    # ── LEFT CELL: Subject Tables ──

    # Title
    p_title = left.paragraphs[0]
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_title.paragraph_format.space_before = Pt(3)
    p_title.paragraph_format.space_after = Pt(3)
    r = p_title.add_run("Detail of Subjects Valued (Issue Reg. Pg.No: 1)")
    r.font.size = Pt(10); r.font.bold = True; r.font.name = FONT; r.font.underline = True

    # Create FN/AN side-by-side via a 1×2 borderless nested layout table
    layout_tbl = doc.add_table(rows=1, cols=2)
    layout_tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    for c in layout_tbl.rows[0].cells: no_borders(c)
    # Move into left cell
    left._tc.append(layout_tbl._tbl)

    fn_container = layout_tbl.rows[0].cells[0]
    an_container = layout_tbl.rows[0].cells[1]
    fn_container.width = Cm(7.0)
    an_container.width = Cm(7.0)

    def build_script_table(container, session_label):
        """Build a bordered FN or AN script table with 3 columns and NUM_ROWS data rows."""
        # Session label
        p_lbl = container.paragraphs[0] if container.paragraphs else container.add_paragraph()
        p_lbl.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_lbl.paragraph_format.space_before = Pt(0)
        p_lbl.paragraph_format.space_after = Pt(0)

        # Create table: 1 header row + NUM_ROWS data rows = NUM_ROWS+2 (incl session label row)
        tbl = doc.add_table(rows=NUM_ROWS + 2, cols=3)
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        for row in tbl.rows:
            for cell in row.cells:
                all_borders(cell)

        # Row 0: Session label (merge all 3 cols)
        tbl.rows[0].cells[0].merge(tbl.rows[0].cells[2])
        fmt(tbl.rows[0].cells[0], session_label, bold=True, sz=10)
        set_row_height(tbl.rows[0], 0.4)

        # Row 1: Column headers
        fmt(tbl.rows[1].cells[0], "S.N.", bold=True, sz=8)
        fmt(tbl.rows[1].cells[1], "Subject Code", bold=True, sz=8)
        fmt(tbl.rows[1].cells[2], "No. of\nScripts", bold=True, sz=8)
        set_row_height(tbl.rows[1], 0.5)

        # Data rows
        for i in range(NUM_ROWS):
            row = tbl.rows[i + 2]
            fmt(row.cells[0], str(i + 1), sz=8)
            fmt(row.cells[1], "", sz=8)
            fmt(row.cells[2], "", sz=8)
            set_row_height(row, 0.38)

        # Column widths
        for row in tbl.rows:
            set_cell_width(row.cells[0], 1.0)
            set_cell_width(row.cells[1], 3.8)
            set_cell_width(row.cells[2], 1.8)

        container._tc.append(tbl._tbl)

    build_script_table(fn_container, "FN")
    build_script_table(an_container, "AN")

    # ── RIGHT CELL: Detail of Claim Amount ──

    p_claim = right.paragraphs[0]
    p_claim.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_claim.paragraph_format.space_before = Pt(3)
    p_claim.paragraph_format.space_after = Pt(4)
    r = p_claim.add_run("Detail of Claim Amount")
    r.font.size = Pt(12); r.font.bold = True; r.font.name = FONT; r.font.underline = True

    # Claim table: 4 rows × 2 cols (Description | Rate)
    ct = doc.add_table(rows=4, cols=2)
    ct.alignment = WD_TABLE_ALIGNMENT.CENTER
    for row in ct.rows:
        for cell in row.cells:
            all_borders(cell)
    right._tc.append(ct._tbl)

    # Row 0: Script Amount
    fmt(ct.rows[0].cells[0], "Script Amount (Rs.)\n(8 Scripts)", bold=True, sz=9,
        align=WD_ALIGN_PARAGRAPH.LEFT)
    fmt(ct.rows[0].cells[1], "UG and PG : 30/script\n(Min. Rs.100/- per subject)", sz=8.5,
        align=WD_ALIGN_PARAGRAPH.LEFT)

    # Row 1: Travelling Allowance
    fmt(ct.rows[1].cells[0], "Travelling Allowance (Rs.)\n(0 Km)", bold=True, sz=9,
        align=WD_ALIGN_PARAGRAPH.LEFT)
    fmt(ct.rows[1].cells[1], "Rs. 8/Km (To and Fro)\nRs. 150 (Up to 35 Km)", sz=8.5,
        align=WD_ALIGN_PARAGRAPH.LEFT)

    # Row 2: Dearness Allowance
    fmt(ct.rows[2].cells[0], "Dearness Allowance (Rs.)\n(Session Only AN: Int./Ext. : INT.)", bold=True, sz=8.5,
        align=WD_ALIGN_PARAGRAPH.LEFT)
    fmt(ct.rows[2].cells[1], "Rs.300/Day\nRs. 250/Session", sz=8.5,
        align=WD_ALIGN_PARAGRAPH.LEFT)

    # Row 3: Total Amount (bold, merged)
    fmt(ct.rows[3].cells[0], "Total Amount (Rs.)", bold=True, sz=11,
        align=WD_ALIGN_PARAGRAPH.LEFT)
    fmt(ct.rows[3].cells[1], "", sz=9)

    # Column widths and row heights
    for row in ct.rows:
        set_cell_width(row.cells[0], 6.0)
        set_cell_width(row.cells[1], 6.5)
        set_row_height(row, 0.9)

    # Received line
    p_recv = right.add_paragraph()
    p_recv.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p_recv.paragraph_format.space_before = Pt(3)
    p_recv.paragraph_format.space_after = Pt(0)
    r = p_recv.add_run("Received Rs. 380/- (Rupees Three Hundred Eighty Only Only)")
    r.font.size = Pt(9); r.font.bold = False; r.font.name = FONT

    # ── RIGHT SIGNATURE CELL ──
    right_sig.vertical_alignment = WD_ALIGN_VERTICAL.BOTTOM

    p_ex = right_sig.paragraphs[0]
    p_ex.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_ex.paragraph_format.space_before = Pt(12)
    p_ex.paragraph_format.space_after = Pt(1)
    r = p_ex.add_run("Signature of the Examiner")
    r.font.size = Pt(10); r.font.bold = True; r.font.name = FONT

    p_stamp = right_sig.add_paragraph()
    p_stamp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_stamp.paragraph_format.space_before = Pt(0)
    p_stamp.paragraph_format.space_after = Pt(2)
    r = p_stamp.add_run("(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)")
    r.font.size = Pt(7.5); r.font.italic = True; r.font.name = FONT

    # Left signature cell empty
    fmt(left_sig, "", sz=6)

    # ══════════════════════════════════════════════════════════════════════
    # 4. BANK DETAILS TABLE (4 rows × 3 cols, col 3 vertically merged)
    # ══════════════════════════════════════════════════════════════════════
    bank = doc.add_table(rows=4, cols=3)
    bank.alignment = WD_TABLE_ALIGNMENT.CENTER
    for row in bank.rows:
        for cell in row.cells:
            all_borders(cell)

    # Vertically merge col 2 (Chief Examiner signature)
    vmerge_restart(bank.rows[0].cells[2])
    for i in range(1, 4):
        vmerge_continue(bank.rows[i].cells[2])

    labels = [
        ("ACCOUNT NUMBER :", ""),
        ("IFSC CODE :", ""),
        ("BANK NAME :", ""),
        ("BRANCH :", ""),
    ]
    for i, (lbl, val) in enumerate(labels):
        fmt(bank.rows[i].cells[0], lbl, bold=True, sz=10, align=WD_ALIGN_PARAGRAPH.LEFT)
        fmt(bank.rows[i].cells[1], val, sz=10, align=WD_ALIGN_PARAGRAPH.LEFT)
        set_row_height(bank.rows[i], 0.55)

    # Chief Examiner signature cell
    sig_cell = bank.rows[0].cells[2]
    sig_cell.vertical_alignment = WD_ALIGN_VERTICAL.BOTTOM
    p_chief = sig_cell.paragraphs[0]
    p_chief.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_chief.paragraph_format.space_before = Pt(0)
    p_chief.paragraph_format.space_after = Pt(4)
    r = p_chief.add_run("Signature of the Chief Examiner")
    r.font.size = Pt(10); r.font.bold = True; r.font.name = FONT

    # Bank table column widths
    for row in bank.rows:
        set_cell_width(row.cells[0], 4.5)
        set_cell_width(row.cells[1], 12.3)
        set_cell_width(row.cells[2], 11.0)

    # ══════════════════════════════════════════════════════════════════════
    # SAVE
    # ══════════════════════════════════════════════════════════════════════
    doc.save(output_path)
    print(f"[OK] Exact revaluation form generated: {output_path}")
    print(f"     FN: {NUM_ROWS} rows | AN: {NUM_ROWS} rows")
    print(f"     3 columns per table: S.N., Subject Code, No. of Scripts")
    print(f"     Layout: A4 Landscape, single bordered container")

if __name__ == "__main__":
    out = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "KRCE_ESE_Revaluation_Dec_2025_A4_Landscape_15_FN_AN_FINAL.docx"
    )
    generate(out)
