"""
Exact Revaluation Claim Form Generator for KRCE COE Office
Generates: KRCE_ESE_Revaluation_Dec_2025_A4_Landscape_15_FN_AN_FINAL.docx
- A4 Landscape layout matching the exact scanned form
- 15 rows in both FN and AN subject tables
- Exact logo placement, titles, borders, calculation details, bank table, and signatures
"""

import os
from docx import Document
from docx.shared import Inches, Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_ORIENT, WD_SECTION_START
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml.ns import qn, nsdecls
from docx.oxml import parse_xml, OxmlElement

NUM_ROWS = 15  # 15 serial number rows in each FN and AN table

def set_cell_border(cell, **kwargs):
    """Set individual cell borders."""
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(f'<w:tcBorders {nsdecls("w")}></w:tcBorders>')
    for edge, attrs in kwargs.items():
        element = parse_xml(
            f'<w:{edge} {nsdecls("w")} w:val="{attrs.get("val", "single")}" '
            f'w:sz="{attrs.get("sz", 4)}" w:space="0" w:color="{attrs.get("color", "000000")}"/>'
        )
        tcBorders.append(element)
    tcPr.append(tcBorders)

def set_cell_shading(cell, color):
    """Set background fill color for cell."""
    shading_elm = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{color}"/>')
    cell._tc.get_or_add_tcPr().append(shading_elm)

def set_cell_width(cell, width_cm):
    """Set cell width in cm."""
    cell.width = Cm(width_cm)

def fmt_cell(cell, text, bold=False, italic=False, size=8, alignment=WD_ALIGN_PARAGRAPH.CENTER, font_name='Times New Roman', space_after=0):
    """Format cell text and alignment cleanly."""
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = alignment
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = Pt(10)
    
    if text:
        lines = str(text).split('\n')
        for i, line in enumerate(lines):
            if i > 0:
                p = cell.add_paragraph()
                p.alignment = alignment
                p.paragraph_format.space_before = Pt(0)
                p.paragraph_format.space_after = Pt(space_after)
                p.paragraph_format.line_spacing = Pt(10)
            run = p.add_run(line)
            run.font.size = Pt(size)
            run.font.bold = bold
            run.font.italic = italic
            run.font.name = font_name
            
    cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER

def add_bordered_table(doc, rows, cols):
    """Create a table with standard 0.5pt single black borders."""
    table = doc.add_table(rows=rows, cols=cols)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for row in table.rows:
        for cell in row.cells:
            set_cell_border(
                cell,
                top={"sz": 4, "val": "single"},
                bottom={"sz": 4, "val": "single"},
                start={"sz": 4, "val": "single"},
                end={"sz": 4, "val": "single"},
            )
    return table

def set_row_height(row, height_cm):
    """Set row height."""
    trPr = row._tr.get_or_add_trPr()
    trHeight = parse_xml(f'<w:trHeight {nsdecls("w")} w:val="{int(height_cm * 567)}" w:hRule="atLeast"/>')
    trPr.append(trHeight)

def generate_exact_revaluation_docx(output_path):
    doc = Document()

    # --- Page setup: A4 Landscape ---
    section = doc.sections[0]
    section.orientation = WD_ORIENT.LANDSCAPE
    section.page_width = Cm(29.7)
    section.page_height = Cm(21.0)
    section.top_margin = Cm(0.8)
    section.bottom_margin = Cm(0.8)
    section.left_margin = Cm(1.0)
    section.right_margin = Cm(1.0)

    # =========================================================================
    # HEADER SECTION (Table with Logo & Title)
    # =========================================================================
    header_table = doc.add_table(rows=1, cols=2)
    header_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    header_table.autofit = False

    # Left cell: Logo + College Info
    c0 = header_table.rows[0].cells[0]
    c0.width = Cm(20.0)
    
    p = c0.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(1)

    # Embed logo if available
    logo_path = os.path.join(os.path.dirname(output_path), "src", "main", "resources", "logo1.png")
    if os.path.exists(logo_path):
        try:
            run_img = p.add_run()
            run_img.add_picture(logo_path, width=Cm(1.4))
        except Exception:
            pass

    r = p.add_run("\nOffice of the Controller of Examinations")
    r.font.size = Pt(12)
    r.font.bold = True
    r.font.name = 'Times New Roman'

    p2 = c0.add_paragraph()
    p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p2.paragraph_format.space_before = Pt(0)
    p2.paragraph_format.space_after = Pt(0)
    r = p2.add_run("K.RAMAKRISHNAN COLLEGE OF ENGINEERING")
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.name = 'Times New Roman'

    p3 = c0.add_paragraph()
    p3.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p3.paragraph_format.space_before = Pt(0)
    p3.paragraph_format.space_after = Pt(1)
    r = p3.add_run("(Autonomous)")
    r.font.size = Pt(10)
    r.font.name = 'Times New Roman'

    p4 = c0.add_paragraph()
    p4.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p4.paragraph_format.space_before = Pt(1)
    p4.paragraph_format.space_after = Pt(2)
    r = p4.add_run("ESE - Re-Valuation - DEC 2025 Examinations")
    r.font.size = Pt(11)
    r.font.bold = True
    r.font.name = 'Times New Roman'
    r.font.underline = True

    # Right cell: Date & Sessions metadata
    c1 = header_table.rows[0].cells[1]
    c1.width = Cm(7.7)
    c1.vertical_alignment = WD_ALIGN_VERTICAL.BOTTOM
    
    p_meta = c1.paragraphs[0]
    p_meta.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p_meta.paragraph_format.space_before = Pt(0)
    p_meta.paragraph_format.space_after = Pt(2)
    
    r = p_meta.add_run("Date of Valuation(dd-mm-yyyy) : __________\n")
    r.font.size = Pt(9)
    r.font.bold = True
    r.font.name = 'Times New Roman'

    r2 = p_meta.add_run("No. of Sessions : Only AN")
    r2.font.size = Pt(9)
    r2.font.bold = True
    r2.font.name = 'Times New Roman'

    # Remove borders from header_table
    for cell in header_table.rows[0].cells:
        set_cell_border(cell, top={"sz":0,"val":"none"}, bottom={"sz":0,"val":"none"}, start={"sz":0,"val":"none"}, end={"sz":0,"val":"none"})

    # =========================================================================
    # PERSONAL DETAILS TABLE
    # =========================================================================
    details_table = add_bordered_table(doc, 2, 8)
    
    # Row 0: Post Held, EXAMINER, Name, Mobile Number, Designation, PROFESSOR
    fmt_cell(details_table.rows[0].cells[0], "Post Held", bold=True, size=8)
    fmt_cell(details_table.rows[0].cells[1], "EXAMINER", bold=True, size=8)
    fmt_cell(details_table.rows[0].cells[2], "Name", bold=True, size=8)
    fmt_cell(details_table.rows[0].cells[3], "", size=8)
    fmt_cell(details_table.rows[0].cells[4], "Mobile Number", bold=True, size=8)
    fmt_cell(details_table.rows[0].cells[5], "", size=8)
    fmt_cell(details_table.rows[0].cells[6], "Designation", bold=True, size=8)
    fmt_cell(details_table.rows[0].cells[7], "PROFESSOR", bold=True, size=8)

    # Row 1: Name of Institution, Board
    fmt_cell(details_table.rows[1].cells[0], "Name of Institution", bold=True, size=8)
    details_table.rows[1].cells[1].merge(details_table.rows[1].cells[5])
    fmt_cell(details_table.rows[1].cells[1], "", size=8)
    fmt_cell(details_table.rows[1].cells[6], "Board", bold=True, size=8)
    fmt_cell(details_table.rows[1].cells[7], "", size=8)

    set_row_height(details_table.rows[0], 0.55)
    set_row_height(details_table.rows[1], 0.55)

    # Column widths for details table
    col_widths_details = [2.6, 2.5, 1.8, 5.0, 2.8, 3.2, 2.3, 2.5]
    for row in details_table.rows:
        for idx, w in enumerate(col_widths_details):
            if idx < len(row.cells):
                set_cell_width(row.cells[idx], w)

    # Spacer
    p_sp = doc.add_paragraph()
    p_sp.paragraph_format.space_before = Pt(3)
    p_sp.paragraph_format.space_after = Pt(0)

    # =========================================================================
    # MAIN BODY: 2-COLUMN LANDSCAPE TABLE (Left: Subjects Table, Right: Claim Details)
    # =========================================================================
    body_table = doc.add_table(rows=1, cols=2)
    body_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    body_table.autofit = False
    
    left_main = body_table.rows[0].cells[0]
    right_main = body_table.rows[0].cells[1]
    left_main.width = Cm(14.8)
    right_main.width = Cm(12.8)

    set_cell_border(left_main, top={"sz":0,"val":"none"}, bottom={"sz":0,"val":"none"}, start={"sz":0,"val":"none"}, end={"sz":0,"val":"none"})
    set_cell_border(right_main, top={"sz":0,"val":"none"}, bottom={"sz":0,"val":"none"}, start={"sz":0,"val":"none"}, end={"sz":0,"val":"none"})

    # -------------------------------------------------------------------------
    # LEFT MAIN CELL: Detail of Subjects Valued (15 FN & 15 AN rows)
    # -------------------------------------------------------------------------
    p_subj_title = left_main.paragraphs[0]
    p_subj_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_subj_title.paragraph_format.space_before = Pt(0)
    p_subj_title.paragraph_format.space_after = Pt(3)
    r = p_subj_title.add_run("Detail of Subjects Valued (Issue Reg. Pg.No: 1)")
    r.font.size = Pt(9.5)
    r.font.bold = True
    r.font.name = 'Times New Roman'
    r.font.underline = True

    # 17 rows x 9 columns table (FN: 4 cols, Gap: 1 col, AN: 4 cols)
    subj_table = add_bordered_table(doc, NUM_ROWS + 2, 9)
    # Move table XML into left_main cell
    left_main._tc.append(subj_table._tbl)

    # Row 0: FN (cols 0-3) and AN (cols 5-8)
    subj_table.rows[0].cells[0].merge(subj_table.rows[0].cells[3])
    fmt_cell(subj_table.rows[0].cells[0], "FN", bold=True, size=9)
    set_cell_shading(subj_table.rows[0].cells[0], "F2F2F2")

    fmt_cell(subj_table.rows[0].cells[4], "", size=6) # Gap cell

    subj_table.rows[0].cells[5].merge(subj_table.rows[0].cells[8])
    fmt_cell(subj_table.rows[0].cells[5], "AN", bold=True, size=9)
    set_cell_shading(subj_table.rows[0].cells[5], "F2F2F2")

    set_row_height(subj_table.rows[0], 0.45)

    # Row 1: Column Headers
    fn_headers = ["S.N.", "Subject Code", "No. of\nScripts", "Amount\n(Rs.)"]
    an_headers = ["S.N.", "Subject Code", "No. of\nScripts", "Amount\n(Rs.)"]

    for i, h in enumerate(fn_headers):
        fmt_cell(subj_table.rows[1].cells[i], h, bold=True, size=7.5)
        set_cell_shading(subj_table.rows[1].cells[i], "FAFAFA")

    fmt_cell(subj_table.rows[1].cells[4], "", size=6)

    for i, h in enumerate(an_headers):
        fmt_cell(subj_table.rows[1].cells[5 + i], h, bold=True, size=7.5)
        set_cell_shading(subj_table.rows[1].cells[5 + i], "FAFAFA")

    set_row_height(subj_table.rows[1], 0.5)

    # Rows 2 to 16: Serial numbers 1 to 15
    for r_idx in range(NUM_ROWS):
        row = subj_table.rows[r_idx + 2]
        # FN side
        fmt_cell(row.cells[0], str(r_idx + 1), size=7.5)
        fmt_cell(row.cells[1], "", size=7.5)
        fmt_cell(row.cells[2], "", size=7.5)
        fmt_cell(row.cells[3], "", size=7.5)

        # Gap
        fmt_cell(row.cells[4], "", size=6)

        # AN side
        fmt_cell(row.cells[5], str(r_idx + 1), size=7.5)
        fmt_cell(row.cells[6], "", size=7.5)
        fmt_cell(row.cells[7], "", size=7.5)
        fmt_cell(row.cells[8], "", size=7.5)

        set_row_height(row, 0.42)

    # Widths for FN/AN columns (Total ~14.5 cm)
    for row in subj_table.rows:
        set_cell_width(row.cells[0], 0.9)   # S.N.
        set_cell_width(row.cells[1], 2.8)   # Subject Code
        set_cell_width(row.cells[2], 1.5)   # No of Scripts
        set_cell_width(row.cells[3], 1.7)   # Amount
        set_cell_width(row.cells[4], 0.3)   # Gap
        set_cell_width(row.cells[5], 0.9)   # S.N.
        set_cell_width(row.cells[6], 2.8)   # Subject Code
        set_cell_width(row.cells[7], 1.5)   # No of Scripts
        set_cell_width(row.cells[8], 1.7)   # Amount

    # Remove top/bottom borders from gap column
    for row in subj_table.rows:
        set_cell_border(
            row.cells[4],
            top={"sz":0, "val":"none"},
            bottom={"sz":0, "val":"none"},
            start={"sz":4, "val":"single"},
            end={"sz":4, "val":"single"}
        )

    # -------------------------------------------------------------------------
    # RIGHT MAIN CELL: Detail of Claim Amount
    # -------------------------------------------------------------------------
    p_claim_title = right_main.paragraphs[0]
    p_claim_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_claim_title.paragraph_format.space_before = Pt(0)
    p_claim_title.paragraph_format.space_after = Pt(3)
    r = p_claim_title.add_run("Detail of Claim Amount")
    r.font.size = Pt(9.5)
    r.font.bold = True
    r.font.name = 'Times New Roman'
    r.font.underline = True

    # 4 rows x 3 columns claim calculation table
    claim_tbl = add_bordered_table(doc, 4, 3)
    right_main._tc.append(claim_tbl._tbl)

    # Row 0: Script Amount
    fmt_cell(claim_tbl.rows[0].cells[0], "Script Amount (Rs.)\n(8 Scripts)", bold=True, size=8, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_tbl.rows[0].cells[1], "UG and PG : 30/script\n(Min. Rs.100/- per subject)", size=7.5, alignment=WD_ALIGN_PARAGRAPH.CENTER)
    fmt_cell(claim_tbl.rows[0].cells[2], "240", size=8.5, alignment=WD_ALIGN_PARAGRAPH.RIGHT)

    # Row 1: Travelling Allowance
    fmt_cell(claim_tbl.rows[1].cells[0], "Travelling Allowance (Rs.)\n(0 Km)", bold=True, size=8, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_tbl.rows[1].cells[1], "Rs. 8/Km (To and Fro)\nRs. 150 (Up to 35 Km)", size=7.5, alignment=WD_ALIGN_PARAGRAPH.CENTER)
    fmt_cell(claim_tbl.rows[1].cells[2], "0", size=8.5, alignment=WD_ALIGN_PARAGRAPH.RIGHT)

    # Row 2: Dearness Allowance
    fmt_cell(claim_tbl.rows[2].cells[0], "Dearness Allowance (Rs.)\n(Session: Only AN; Int./Ext. : INT.)", bold=True, size=7.5, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_tbl.rows[2].cells[1], "Rs.300/Day\nRs. 250/Session", size=7.5, alignment=WD_ALIGN_PARAGRAPH.CENTER)
    fmt_cell(claim_tbl.rows[2].cells[2], "250", size=8.5, alignment=WD_ALIGN_PARAGRAPH.RIGHT)

    # Row 3: Total Amount
    fmt_cell(claim_tbl.rows[3].cells[0], "Total Amount (Rs.)", bold=True, size=9, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    set_cell_shading(claim_tbl.rows[3].cells[0], "FAFAFA")

    claim_tbl.rows[3].cells[1].merge(claim_tbl.rows[3].cells[2])
    fmt_cell(claim_tbl.rows[3].cells[1], "Received Rs. 490/- (Rupees Four Hundred Ninety Only)", bold=True, size=8, alignment=WD_ALIGN_PARAGRAPH.LEFT)

    for row in claim_tbl.rows:
        set_cell_width(row.cells[0], 5.2)
        set_cell_width(row.cells[1], 5.2)
        if len(row.cells) > 2:
            set_cell_width(row.cells[2], 2.2)
        set_row_height(row, 0.75)

    # Examiner Signature Section under Claim Table
    p_sig_ex = right_main.add_paragraph()
    p_sig_ex.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_sig_ex.paragraph_format.space_before = Pt(14)
    p_sig_ex.paragraph_format.space_after = Pt(1)
    r = p_sig_ex.add_run("Signature of the Examiner")
    r.font.size = Pt(9)
    r.font.bold = True
    r.font.name = 'Times New Roman'

    p_stamp = right_main.add_paragraph()
    p_stamp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_stamp.paragraph_format.space_before = Pt(0)
    p_stamp.paragraph_format.space_after = Pt(6)
    r = p_stamp.add_run("(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)")
    r.font.size = Pt(7.5)
    r.font.italic = True
    r.font.name = 'Times New Roman'

    # =========================================================================
    # BANK DETAILS & CHIEF EXAMINER SIGNATURE TABLE
    # =========================================================================
    p_sp2 = doc.add_paragraph()
    p_sp2.paragraph_format.space_before = Pt(4)
    p_sp2.paragraph_format.space_after = Pt(0)

    bank_table = add_bordered_table(doc, 4, 3)

    # Vertical merge cell 2 (Chief Examiner Signature) across 4 rows
    c_sig_top = bank_table.rows[0].cells[2]
    tcPr = c_sig_top._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:vMerge {nsdecls("w")} w:val="restart"/>'))

    for r_i in range(1, 4):
        c_merge = bank_table.rows[r_i].cells[2]
        tcPr_m = c_merge._tc.get_or_add_tcPr()
        tcPr_m.append(parse_xml(f'<w:vMerge {nsdecls("w")} w:val="continue"/>'))

    # Bank rows
    bank_info = [
        ("ACCOUNT NUMBER :", ""),
        ("IFSC CODE :", ""),
        ("BANK NAME :", ""),
        ("BRANCH :", "")
    ]

    for idx, (label, val) in enumerate(bank_info):
        fmt_cell(bank_table.rows[idx].cells[0], label, bold=True, size=8.5, alignment=WD_ALIGN_PARAGRAPH.LEFT)
        fmt_cell(bank_table.rows[idx].cells[1], val, size=8.5, alignment=WD_ALIGN_PARAGRAPH.LEFT)
        set_cell_shading(bank_table.rows[idx].cells[0], "FAFAFA")
        set_row_height(bank_table.rows[idx], 0.55)

    # Signature cell content
    fmt_cell(c_sig_top, "\n\nSignature of the Chief Examiner", bold=True, size=9, alignment=WD_ALIGN_PARAGRAPH.CENTER)
    c_sig_top.vertical_alignment = WD_ALIGN_VERTICAL.BOTTOM

    # Column widths for Bank table
    for r_i in range(4):
        set_cell_width(bank_table.rows[r_i].cells[0], 4.2)
        set_cell_width(bank_table.rows[r_i].cells[1], 13.5)
        set_cell_width(bank_table.rows[r_i].cells[2], 9.8)

    # Save document
    doc.save(output_path)
    print(f"[OK] Generated document: {output_path}")

if __name__ == "__main__":
    out_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "KRCE_ESE_Revaluation_Dec_2025_A4_Landscape_15_FN_AN_FINAL.docx")
    generate_exact_revaluation_docx(out_path)
