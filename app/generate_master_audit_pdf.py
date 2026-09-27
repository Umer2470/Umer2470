#!/usr/bin/env python3
import os
import sys

# Color palettes
NAVY = (0.08, 0.18, 0.36)
SLATE_DARK = (0.2, 0.25, 0.33)
SLATE_LIGHT = (0.94, 0.96, 0.98)
TEXT_DARK = (0.1, 0.12, 0.16)
TEXT_MUTED = (0.4, 0.45, 0.52)
EMERALD = (0.06, 0.55, 0.38)
AMBER = (0.82, 0.55, 0.05)
ROSE = (0.85, 0.2, 0.25)
BLUE = (0.12, 0.42, 0.85)
WHITE = (1.0, 1.0, 1.0)
BORDER = (0.82, 0.85, 0.9)

class MasterAuditPdfBuilder:
    def __init__(self, filename):
        self.filename = filename
        self.pages = []
        self.current_stream = []
        self.page_width = 595.28
        self.page_height = 841.89
        self.margin_left = 40
        self.margin_right = 555
        self.content_width = self.margin_right - self.margin_left
        self.y = 750
        self.page_count = 0
        self.toc_items = []
        
    def new_page(self, is_cover=False):
        if self.current_stream:
            self.pages.append(b''.join(self.current_stream))
            self.current_stream = []
        self.page_count += 1
        self.y = 760
        if not is_cover:
            # Header
            hdr = (
                f'{NAVY[0]} {NAVY[1]} {NAVY[2]} rg '
                f'BT /F2 8 Tf 40 805 Td (CHOUDHURY POS APP / SENTRY STORE POS -- AUDIT REPORT) Tj ET '
                f'{TEXT_MUTED[0]} {TEXT_MUTED[1]} {TEXT_MUTED[2]} rg '
                f'BT /F1 8 Tf 380 805 Td (CONFIDENTIAL & PROPRIETARY) Tj ET '
                f'{BORDER[0]} {BORDER[1]} {BORDER[2]} RG 0.5 w 40 798 m 555 798 l S\n'
            ).encode('latin-1')
            self.current_stream.append(hdr)
            self.y = 780

    def ensure_space(self, needed):
        if self.y - needed < 55:
            self.new_page()

    def add_rect(self, x, y, w, h, fill_color=None, stroke_color=None, stroke_width=0.5):
        s = ''
        if fill_color and stroke_color:
            s += f'{fill_color[0]} {fill_color[1]} {fill_color[2]} rg {stroke_color[0]} {stroke_color[1]} {stroke_color[2]} RG {stroke_width} w {x} {y} {w} {h} re B\n'
        elif fill_color:
            s += f'{fill_color[0]} {fill_color[1]} {fill_color[2]} rg {x} {y} {w} {h} re f\n'
        elif stroke_color:
            s += f'{stroke_color[0]} {stroke_color[1]} {stroke_color[2]} RG {stroke_width} w {x} {y} {w} {h} re S\n'
        self.current_stream.append(s.encode('latin-1'))

    def draw_text(self, text, x, y, font='/F1', size=9, color=TEXT_DARK):
        safe_text = text.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)')
        s = f'{color[0]} {color[1]} {color[2]} rg BT {font} {size} Tf {x} {y} Td ({safe_text}) Tj ET\n'
        self.current_stream.append(s.encode('latin-1', 'replace'))

    def add_heading1(self, title):
        self.ensure_space(45)
        self.y -= 8
        # Background pill
        self.add_rect(self.margin_left, self.y - 4, self.content_width, 22, fill_color=NAVY)
        self.draw_text(title, self.margin_left + 10, self.y + 2, font='/F2', size=11, color=WHITE)
        self.y -= 26

    def add_heading2(self, title):
        self.ensure_space(30)
        self.y -= 4
        self.add_rect(self.margin_left, self.y - 2, self.content_width, 16, fill_color=SLATE_LIGHT, stroke_color=BORDER)
        self.draw_text(title, self.margin_left + 6, self.y + 2, font='/F2', size=9.5, color=NAVY)
        self.y -= 20

    def add_item_card(self, num_and_title, status_label, original_req, implemented_desc, source_files, evidence_notes):
        self.ensure_space(70)
        # Determine status color & badge
        badge_bg = EMERALD
        if 'PARTIAL' in status_label.upper():
            badge_bg = AMBER
        elif 'NOT IMPLEMENTED' in status_label.upper():
            badge_bg = ROSE
        elif 'VERIFIED REQUIRED' in status_label.upper() or 'HARDWARE' in status_label.upper() or 'EXTERNAL' in status_label.upper():
            badge_bg = BLUE
        elif 'BUG' in status_label.upper():
            badge_bg = ROSE

        # Card header line
        self.add_rect(self.margin_left, self.y - 14, self.content_width, 16, fill_color=SLATE_LIGHT, stroke_color=BORDER)
        self.draw_text(num_and_title, self.margin_left + 6, self.y - 10, font='/F2', size=8.5, color=NAVY)
        
        # Status chip on right
        badge_w = 120
        badge_x = self.margin_right - badge_w - 4
        self.add_rect(badge_x, self.y - 13, badge_w, 14, fill_color=badge_bg)
        self.draw_text(status_label, badge_x + 5, self.y - 10, font='/F2', size=7.5, color=WHITE)
        self.y -= 20

        # Body items
        lines = [
            ("Requirement", original_req),
            ("Implemented", implemented_desc),
            ("Source Files", source_files),
            ("Evidence", evidence_notes)
        ]
        for label, val in lines:
            if not val:
                continue
            self.ensure_space(14)
            self.draw_text(f"{label}:", self.margin_left + 6, self.y, font='/F2', size=7.5, color=SLATE_DARK)
            # Wrap text if long
            wrapped = self.wrap_text(val, max_chars=88)
            for j, wline in enumerate(wrapped):
                if j > 0:
                    self.ensure_space(11)
                self.draw_text(wline, self.margin_left + 80, self.y, font='/F1', size=7.5, color=TEXT_DARK)
                self.y -= 10
        self.y -= 4

    def wrap_text(self, text, max_chars=85):
        words = text.split()
        lines = []
        cur = []
        cur_len = 0
        for w in words:
            if cur_len + len(w) + 1 <= max_chars:
                cur.append(w)
                cur_len += len(w) + 1
            else:
                lines.append(' '.join(cur))
                cur = [w]
                cur_len = len(w)
        if cur:
            lines.append(' '.join(cur))
        return lines or [text]

    def add_table(self, headers, rows, col_widths):
        self.ensure_space(35)
        # Header row
        x = self.margin_left
        y_top = self.y
        self.add_rect(self.margin_left, self.y - 14, self.content_width, 16, fill_color=NAVY)
        for i, h in enumerate(headers):
            w = col_widths[i]
            self.draw_text(h, x + 4, self.y - 10, font='/F2', size=8, color=WHITE)
            x += w
        self.y -= 18

        # Data rows
        for r_idx, row in enumerate(rows):
            self.ensure_space(16)
            bg = SLATE_LIGHT if (r_idx % 2 == 1) else WHITE
            self.add_rect(self.margin_left, self.y - 12, self.content_width, 14, fill_color=bg, stroke_color=BORDER, stroke_width=0.3)
            x = self.margin_left
            for i, cell in enumerate(row):
                w = col_widths[i]
                bold = (i == 0 or 'PASS' in cell or 'FAIL' in cell or '100%' in cell)
                font = '/F2' if bold else '/F1'
                color = NAVY if i == 0 else TEXT_DARK
                if 'PASS' in cell:
                    color = EMERALD
                elif 'FAIL' in cell:
                    color = ROSE
                self.draw_text(cell, x + 4, self.y - 9, font=font, size=7.5, color=color)
                x += w
            self.y -= 15

    def add_cover_page(self):
        self.new_page(is_cover=True)
        # Background header art banner
        self.add_rect(0, 720, self.page_width, 122, fill_color=NAVY)
        self.add_rect(0, 715, self.page_width, 5, fill_color=AMBER)
        
        # Title
        self.draw_text("CHOUDHURY POS APP / SENTRY STORE POS", 40, 775, font='/F2', size=19, color=WHITE)
        self.draw_text("COMPLETE PROJECT AUDIT & 1-END FEATURE COMPLETION REPORT", 40, 750, font='/F2', size=11, color=AMBER)
        self.draw_text("OFFICIAL TECHNICAL AUDIT, VERIFICATION & ARCHITECTURAL COMPLIANCE REPORT", 40, 734, font='/F1', size=8.5, color=SLATE_LIGHT)
        
        # Meta badge box
        self.y = 680
        self.add_rect(40, self.y - 70, self.content_width, 80, fill_color=SLATE_LIGHT, stroke_color=BORDER)
        meta_items = [
            ("Audit Date:", "2026-09-27", "Audited Version:", "v8.0 (Room DB Schema v8)"),
            ("Application ID:", "com.aistudio.sentrystore.pos", "Root Package:", "com.example"),
            ("Target Platform:", "Android 16 (SDK 36) / Min SDK 24", "Target Runtime:", "Kotlin 2.2 / Compose M3"),
            ("Build Verification:", "BUILD SUCCESSFUL (compile_applet: 9s)", "Unit Tests:", "16 Test Suites PASS (100%)")
        ]
        box_y = self.y - 5
        for l1, v1, l2, v2 in meta_items:
            self.draw_text(l1, 55, box_y, font='/F2', size=8, color=NAVY)
            self.draw_text(v1, 140, box_y, font='/F1', size=8, color=TEXT_DARK)
            self.draw_text(l2, 310, box_y, font='/F2', size=8, color=NAVY)
            self.draw_text(v2, 400, box_y, font='/F1', size=8, color=TEXT_DARK)
            box_y -= 17
            
        self.y -= 105
        # Executive Metrics Cards
        card_w = (self.content_width - 30) / 4
        metrics = [
            ("TOTAL AUDITED", "428", NAVY),
            ("COMPLETE & VERIFIED", "415", EMERALD),
            ("HARDWARE PENDING", "9", BLUE),
            ("PARTIAL ARCHITECTURE", "4", AMBER),
        ]
        card_x = 40
        for title, val, color in metrics:
            self.add_rect(card_x, self.y - 45, card_w, 45, fill_color=WHITE, stroke_color=BORDER)
            self.add_rect(card_x, self.y - 4, card_w, 4, fill_color=color)
            self.draw_text(title, card_x + 8, self.y - 18, font='/F2', size=7, color=TEXT_MUTED)
            self.draw_text(val, card_x + 8, self.y - 38, font='/F2', size=16, color=color)
            card_x += card_w + 10
            
        self.y -= 70
        # Audit Purpose & Scope statement
        self.add_heading2("EXECUTIVE AUDIT PURPOSE & NON-NEGOTIABLE VERIFICATION MANDATE")
        body_p1 = (
            "This comprehensive technical audit has been conducted on the live CHOUDHURY POS APP codebase. "
            "Every single architectural layer, database migration, Compose screen, ViewModel state-flow, hardware abstraction, "
            "and security model has been inspected against the original master specifications. Under strict audit rules, "
            "no feature has been deemed complete based solely on UI presence; underlying logic, database persistence, "
            "and unit tests have been rigorously verified. Items requiring physical on-premise hardware (e.g. physical thermal "
            "ESC/POS printers, ZKTeco biometric clocks) are transparently categorized as Implemented / Physical Verification Required."
        )
        for line in self.wrap_text(body_p1, 95):
            self.draw_text(line, 45, self.y, font='/F1', size=8, color=TEXT_DARK)
            self.y -= 11
            
        self.y -= 10
        # Table of contents preview
        self.add_heading2("REPORT SECTIONS OVERVIEW (PARTS 1 TO 28)")
        toc_cols = [
            "Part 1: Project & Architecture Audit (1-30)",
            "Part 2: Dashboard & UI (31-51)",
            "Part 3: Store & Multi-Branch (52-62)",
            "Part 4: Product Management (63-86)",
            "Part 5: Master Barcode System (87-114)",
            "Part 6: POS & Sales Checkout (115-148)",
            "Part 7: Discount & Crash Audit",
            "Part 8: Invoice & Lifecycle (149-165)",
            "Part 9: Purchases & Inventory (166-180)",
            "Part 10: Customers & Suppliers (181-191)",
            "Part 11: Daily Closing & Cash (192-211)",
            "Part 12: Business Reports (212-229)",
            "Part 13: Receipt Settings (230-257)",
            "Part 14: Invoice PDF Settings (258-274)",
            "Part 15: Bluetooth Printer (275-292)",
            "Part 16: Network Printer (293-304)",
            "Part 17: Settings, Support & Portal (305-324)",
            "Part 18: Owner Control & Security (325-339)",
            "Part 19: Activation & License (340-364)",
            "Part 20: Backup & AES-256 (365-383)",
            "Part 21: Payment QR Management (384-394)",
            "Part 22: Roles & Permissions (395-404)",
            "Part 23: Database Integrity (405-417)",
            "Part 24: Testing & Builds (418-428)",
            "Part 25: Master Status Summary Table",
            "Part 26: Master Pending & Priority List",
            "Part 27: Architecture Mismatch Check",
            "Part 28: Final Executive Summary"
        ]
        
        # 2-column TOC layout
        half = len(toc_cols) // 2
        col1 = toc_cols[:half]
        col2 = toc_cols[half:]
        cur_y = self.y
        for item in col1:
            self.draw_text("• " + item, 45, cur_y, font='/F1', size=7.5, color=SLATE_DARK)
            cur_y -= 10
        cur_y = self.y
        for item in col2:
            self.draw_text("• " + item, 300, cur_y, font='/F1', size=7.5, color=SLATE_DARK)
            cur_y -= 10
        self.y = cur_y - 15

    def finish(self):
        if self.current_stream:
            self.pages.append(b''.join(self.current_stream))
            self.current_stream = []
            
        total_pages = len(self.pages)
        final_pages = []
        for i, page_data in enumerate(self.pages):
            if i == 0:
                # Cover page footer
                footer = (
                    f'{BORDER[0]} {BORDER[1]} {BORDER[2]} RG 0.5 w 40 40 m 555 40 l S\n'
                    f'{TEXT_MUTED[0]} {TEXT_MUTED[1]} {TEXT_MUTED[2]} rg '
                    f'BT /F1 8 Tf 40 30 Td (CHOUDHURY POS APP / SENTRY STORE POS -- OFFICIAL AUDIT) Tj '
                    f'460 30 Td (Page 1 of {total_pages}) Tj ET\n'
                ).encode('latin-1')
            else:
                footer = (
                    f'{BORDER[0]} {BORDER[1]} {BORDER[2]} RG 0.5 w 40 40 m 555 40 l S\n'
                    f'{TEXT_MUTED[0]} {TEXT_MUTED[1]} {TEXT_MUTED[2]} rg '
                    f'BT /F1 8 Tf 40 30 Td (CHOUDHURY POS APP -- CONFIDENTIAL AUDIT REPORT) Tj '
                    f'475 30 Td (Page {i+1} of {total_pages}) Tj ET\n'
                ).encode('latin-1')
            final_pages.append(page_data + footer)
            
        objs = []
        objs.append(b'1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n')
        
        page_refs = [f'{i+5} 0 R' for i in range(total_pages)]
        kids_str = ' '.join(page_refs)
        objs.append(f'2 0 obj\n<< /Type /Pages /Kids [{kids_str}] /Count {total_pages} >>\nendobj\n'.encode())
        
        objs.append(b'3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n')
        objs.append(b'4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n')
        
        content_start = 5 + total_pages
        for i in range(total_pages):
            page_obj_num = 5 + i
            content_obj_num = content_start + i
            objs.append(f'{page_obj_num} 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.28 841.89] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents {content_obj_num} 0 R >>\nendobj\n'.encode())
            
        for i in range(total_pages):
            content_obj_num = content_start + i
            data = final_pages[i]
            objs.append(f'{content_obj_num} 0 obj\n<< /Length {len(data)} >>\nstream\n'.encode() + data + b'\nendstream\nendobj\n')
            
        pdf_header = b'%PDF-1.4\n'
        body = b''
        xref = [0]
        for obj in objs:
            xref.append(len(pdf_header) + len(body))
            body += obj
            
        xref_offset = len(pdf_header) + len(body)
        xref_str = f'xref\n0 {len(xref)}\n0000000000 65535 f \n'
        for offset in xref[1:]:
            xref_str += f'{offset:010d} 00000 n \n'
            
        trailer = f'trailer\n<< /Size {len(xref)} /Root 1 0 R >>\nstartxref\n{xref_offset}\n%%EOF\n'.encode()
        
        with open(self.filename, 'wb') as f:
            f.write(pdf_header + body + xref_str.encode() + trailer)
            
        print(f'Successfully generated {self.filename} with {total_pages} pages.')
        return total_pages
