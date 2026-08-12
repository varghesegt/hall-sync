"""
Revaluation Claim Form Generator for KRCE COE Office
Generates a Word document (.docx) matching the official revaluation claim format.
- 15 rows in both FN and AN subject tables
- Calculation details for script amount, TA, DA
"""

from docx import Document
from docx.shared import Inches, Pt, Cm, Emu, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml.ns import qn, nsdecls
from docx.oxml import parse_xml
import os

NUM_ROWS = 15  # 15 serial number rows in each FN/AN table

def set_cell_border(cell, **kwargs):
    """Set cell border. Usage: set_cell_border(cell, top={"sz": 4, "val": "single"}, ...)"""
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcBorders = parse_xml(f'<w:tcBorders {nsdecls("w")}></w:tcBorders>')
    for edge, attrs in kwargs.items():
        element = parse_xml(
            f'<w:{edge} {nsdecls("w")} w:val="{attrs.get("val", "single")}" '
            f'w:sz="{attrs.get("sz", 4)}" w:space="0" w:color="{attrs.get("color", "000000")}"/>'
        )
        tcBorders.append(element)
    tcPr.append(tcBorders)

def set_cell_shading(cell, color):
    """Set cell background color."""
    shading_elm = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{color}"/>')
    cell._tc.get_or_add_tcPr().append(shading_elm)

def set_cell_width(cell, width_cm):
    """Set cell width in cm."""
    cell.width = Cm(width_cm)

def fmt_cell(cell, text, bold=False, size=8, alignment=WD_ALIGN_PARAGRAPH.CENTER, font_name='Arial'):
    """Format a cell with text and styling."""
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = alignment
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = Pt(10)
    run = p.add_run(str(text))
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.name = font_name
    cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER

def add_bordered_table(doc, rows, cols):
    """Create a table with all borders."""
    table = doc.add_table(rows=rows, cols=cols)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    # Set borders for all cells
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
    tr = row._tr
    trPr = tr.get_or_add_trPr()
    trHeight = parse_xml(f'<w:trHeight {nsdecls("w")} w:val="{int(height_cm * 567)}" w:hRule="atLeast"/>')
    trPr.append(trHeight)

def generate_revaluation_form(output_path):
    doc = Document()

    # --- Page setup ---
    section = doc.sections[0]
    section.page_width = Cm(21)    # A4
    section.page_height = Cm(29.7) # A4
    section.top_margin = Cm(1.0)
    section.bottom_margin = Cm(0.8)
    section.left_margin = Cm(1.2)
    section.right_margin = Cm(1.2)

    # ========================================
    # HEADER SECTION
    # ========================================
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run("Office of the Controller of Examinations")
    run.font.size = Pt(12)
    run.font.bold = True
    run.font.name = 'Arial'

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run("KRAMAKRISHNAN COLLEGE OF ENGINEERING")
    run.font.size = Pt(13)
    run.font.bold = True
    run.font.name = 'Arial'

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run("(Autonomous)")
    run.font.size = Pt(10)
    run.font.name = 'Arial'

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    run = p.add_run("ESE - Re-Valuation - DEC 2025 Examinations")
    run.font.size = Pt(11)
    run.font.bold = True
    run.font.name = 'Arial'
    run.font.underline = True

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(4)
    run = p.add_run("No. of Sessions : Only AN")
    run.font.size = Pt(9)
    run.font.bold = True
    run.font.name = 'Arial'

    # ========================================
    # PERSONAL DETAILS TABLE
    # ========================================
    details_table = add_bordered_table(doc, 3, 8)

    # Row 1: Post Held, Date of Valuation
    fmt_cell(details_table.rows[0].cells[0], "Post Held", bold=True, size=8)
    fmt_cell(details_table.rows[0].cells[1], "EXAMINER", bold=True, size=8)
    # Merge cells 2-3 for Date label
    details_table.rows[0].cells[2].merge(details_table.rows[0].cells[3])
    fmt_cell(details_table.rows[0].cells[2], "Date of Valuation(dd.mm.yyyy):", bold=True, size=7)
    # Merge cells 4-7 for date value (empty for filling)
    details_table.rows[0].cells[4].merge(details_table.rows[0].cells[7])
    fmt_cell(details_table.rows[0].cells[4], "", size=8)

    # Row 2: Name, Mobile Number, Designation
    fmt_cell(details_table.rows[1].cells[0], "Name", bold=True, size=8)
    details_table.rows[1].cells[1].merge(details_table.rows[1].cells[2])
    fmt_cell(details_table.rows[1].cells[1], "", size=8)
    fmt_cell(details_table.rows[1].cells[3], "Mobile Number", bold=True, size=7)
    details_table.rows[1].cells[4].merge(details_table.rows[1].cells[5])
    fmt_cell(details_table.rows[1].cells[4], "", size=8)
    fmt_cell(details_table.rows[1].cells[6], "Designation", bold=True, size=7)
    fmt_cell(details_table.rows[1].cells[7], "PROFESSOR", bold=True, size=7)

    # Row 3: Name of Institution, Board
    fmt_cell(details_table.rows[2].cells[0], "Name of Institution", bold=True, size=7)
    details_table.rows[2].cells[1].merge(details_table.rows[2].cells[5])
    fmt_cell(details_table.rows[2].cells[1], "", size=8)
    fmt_cell(details_table.rows[2].cells[6], "Board", bold=True, size=8)
    fmt_cell(details_table.rows[2].cells[7], "", size=8)

    for row in details_table.rows:
        set_row_height(row, 0.6)

    # ========================================
    # SECTION TITLE: Detail of Subjects Valued
    # ========================================
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(3)
    run = p.add_run("Detail of Subjects Valued (Issue Reg. Pg. No: 1)")
    run.font.size = Pt(9)
    run.font.bold = True
    run.font.name = 'Arial'
    run.font.underline = True

    # ========================================
    # FN + AN SUBJECT TABLES (side by side as single table)
    # ========================================
    # Combined table: FN (4 cols) + gap (1 col) + AN (4 cols) = 9 cols
    # Headers + 15 data rows = 17 rows (1 session label + 1 header + 15 data)
    subjects_table = add_bordered_table(doc, NUM_ROWS + 2, 9)

    # Session label row
    # FN header
    subjects_table.rows[0].cells[0].merge(subjects_table.rows[0].cells[3])
    fmt_cell(subjects_table.rows[0].cells[0], "FN", bold=True, size=9)
    set_cell_shading(subjects_table.rows[0].cells[0], "D9E2F3")

    # Gap column (no borders)
    fmt_cell(subjects_table.rows[0].cells[4], "", size=6)

    # AN header
    subjects_table.rows[0].cells[5].merge(subjects_table.rows[0].cells[8])
    fmt_cell(subjects_table.rows[0].cells[5], "AN", bold=True, size=9)
    set_cell_shading(subjects_table.rows[0].cells[5], "D9E2F3")

    # Column headers row
    fn_headers = ["S.N.", "Subject Code", "No. of\nScripts", "Amount\n(Rs.)"]
    an_headers = ["S.N.", "Subject Code", "No. of\nScripts", "Amount\n(Rs.)"]

    for i, h in enumerate(fn_headers):
        fmt_cell(subjects_table.rows[1].cells[i], h, bold=True, size=7)
        set_cell_shading(subjects_table.rows[1].cells[i], "E8E8E8")

    fmt_cell(subjects_table.rows[1].cells[4], "", size=6)

    for i, h in enumerate(an_headers):
        fmt_cell(subjects_table.rows[1].cells[5 + i], h, bold=True, size=7)
        set_cell_shading(subjects_table.rows[1].cells[5 + i], "E8E8E8")

    # Data rows (1-15)
    for row_idx in range(NUM_ROWS):
        data_row = subjects_table.rows[row_idx + 2]
        # FN side
        fmt_cell(data_row.cells[0], str(row_idx + 1), size=7)
        fmt_cell(data_row.cells[1], "", size=7)
        fmt_cell(data_row.cells[2], "", size=7)
        fmt_cell(data_row.cells[3], "", size=7)

        # Gap
        fmt_cell(data_row.cells[4], "", size=6)

        # AN side
        fmt_cell(data_row.cells[5], str(row_idx + 1), size=7)
        fmt_cell(data_row.cells[6], "", size=7)
        fmt_cell(data_row.cells[7], "", size=7)
        fmt_cell(data_row.cells[8], "", size=7)

        set_row_height(data_row, 0.45)

    # Set column widths
    for row in subjects_table.rows:
        set_cell_width(row.cells[0], 0.9)   # S.N.
        set_cell_width(row.cells[1], 3.0)   # Subject Code
        set_cell_width(row.cells[2], 1.5)   # No. of Scripts
        set_cell_width(row.cells[3], 1.8)   # Amount
        set_cell_width(row.cells[4], 0.3)   # Gap
        set_cell_width(row.cells[5], 0.9)   # S.N.
        set_cell_width(row.cells[6], 3.0)   # Subject Code
        set_cell_width(row.cells[7], 1.5)   # No. of Scripts
        set_cell_width(row.cells[8], 1.8)   # Amount

    # Remove borders from gap column
    for row in subjects_table.rows:
        gap_cell = row.cells[4]
        set_cell_border(
            gap_cell,
            top={"sz": 0, "val": "none"},
            bottom={"sz": 0, "val": "none"},
            start={"sz": 4, "val": "single"},
            end={"sz": 4, "val": "single"},
        )

    # ========================================
    # DETAIL OF CLAIM AMOUNT
    # ========================================
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(3)
    run = p.add_run("Detail of Claim Amount")
    run.font.size = Pt(10)
    run.font.bold = True
    run.font.name = 'Arial'
    run.font.underline = True

    # Claim amount table: 4 rows x 3 cols
    claim_table = add_bordered_table(doc, 4, 3)

    # Row 1: Script Amount
    fmt_cell(claim_table.rows[0].cells[0], "Script Amount (Rs.)\n(8 Scripts)", bold=True, size=7, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_table.rows[0].cells[1], "UG and PG: 30/script\n(Min. Rs.100/ per subject)", size=7, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_table.rows[0].cells[2], "", size=8)

    # Row 2: Travelling Allowance
    fmt_cell(claim_table.rows[1].cells[0], "Travelling Allowance (Rs.)\n(0 Km)", bold=True, size=7, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_table.rows[1].cells[1], "Rs. 8/Km (To and Fro)\nRs. 150 (Up to 35 Km)", size=7, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_table.rows[1].cells[2], "", size=8)

    # Row 3: Dearness Allowance
    fmt_cell(claim_table.rows[2].cells[0], "Dearness Allowance (Rs.)\n(Session Only AN: Int./Ext.: INT)", bold=True, size=7, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_table.rows[2].cells[1], "Rs.300/day\nRs. 250/Session", size=7, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    fmt_cell(claim_table.rows[2].cells[2], "", size=8)

    # Row 4: Total Amount
    fmt_cell(claim_table.rows[3].cells[0], "Total Amount (Rs.)", bold=True, size=9, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    claim_table.rows[3].cells[1].merge(claim_table.rows[3].cells[2])
    fmt_cell(claim_table.rows[3].cells[1], "Received Rs. ________/- (Rupees __________________________________ Only)", size=8, alignment=WD_ALIGN_PARAGRAPH.LEFT)
    set_cell_shading(claim_table.rows[3].cells[0], "E8E8E8")

    # Set claim table column widths
    for row in claim_table.rows:
        set_cell_width(row.cells[0], 5.5)
        set_cell_width(row.cells[1], 5.5)
        if len(row.cells) > 2:
            set_cell_width(row.cells[2], 3.5)
        set_row_height(row, 0.8)

    # ========================================
    # SIGNATURE SECTION
    # ========================================
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run("Signature of the Examiner")
    run.font.size = Pt(9)
    run.font.bold = True
    run.font.name = 'Arial'

    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(4)
    run = p.add_run("(Rs. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)")
    run.font.size = Pt(7)
    run.font.italic = True
    run.font.name = 'Arial'

    # ========================================
    # BANK DETAILS TABLE
    # ========================================
    bank_table = add_bordered_table(doc, 4, 2)

    bank_fields = [
        ("ACCOUNT NUMBER :", ""),
        ("IFSC CODE :", ""),
        ("BANK NAME :", ""),
        ("BRANCH :", ""),
    ]

    for i, (label, val) in enumerate(bank_fields):
        fmt_cell(bank_table.rows[i].cells[0], label, bold=True, size=8, alignment=WD_ALIGN_PARAGRAPH.LEFT)
        fmt_cell(bank_table.rows[i].cells[1], val, size=8, alignment=WD_ALIGN_PARAGRAPH.LEFT)
        set_cell_width(bank_table.rows[i].cells[0], 4.0)
        set_cell_width(bank_table.rows[i].cells[1], 10.0)
        set_row_height(bank_table.rows[i], 0.5)

    # ========================================
    # CHIEF EXAMINER SIGNATURE
    # ========================================
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p.paragraph_format.space_before = Pt(16)
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run("Signature of the Chief Examiner")
    run.font.size = Pt(9)
    run.font.bold = True
    run.font.name = 'Arial'

    # ========================================
    # SAVE
    # ========================================
    doc.save(output_path)
    print(f"[OK] Revaluation Claim Form saved to: {output_path}")
    print(f"  - FN table: {NUM_ROWS} rows")
    print(f"  - AN table: {NUM_ROWS} rows")
    print(f"  - Script Rate: UG/PG Rs.30/script (Min Rs.100/subject)")
    print(f"  - TA: Rs.8/Km (To & Fro), Rs.150 up to 35Km")
    print(f"  - DA: Rs.300/day or Rs.250/session")


if __name__ == "__main__":
    output_dir = os.path.dirname(os.path.abspath(__file__))
    output_file = os.path.join(output_dir, "Revaluation_Claim_Form_Template.docx")
    generate_revaluation_form(output_file)
