import { useState } from 'react';
import { motion } from 'framer-motion';
import { Search, FileText, Image, CheckCircle, AlertTriangle, ChevronLeft } from 'lucide-react';
import { PageHeader } from '../../../components/common';
import styles from './CheckerApproval.module.css';

const QUEUE_ITEMS = [
  {
    id: 1,
    rrn: '412908765432',
    time: '2 min ago',
    type: 'P2M Chargeback — FlipMart Pvt Ltd',
    amount: '₹4,500',
    badge: 'early',
    badgeLabel: 'Early',
  },
  {
    id: 2,
    rrn: 'BATCH-20260410-001',
    time: '15 min ago',
    type: 'Bulk Upload — NPCI Adj Report (1,247 records)',
    amount: '₹18.4L Total',
    badge: 'file',
    badgeLabel: 'Bulk File',
  },
  {
    id: 3,
    rrn: '412907654321',
    time: '28 min ago',
    type: 'P2P Adjustment — Evidence Upload',
    amount: '₹12,000',
    badge: 'intermediary',
    badgeLabel: 'Intermediary',
  },
  {
    id: 4,
    rrn: '412906543210',
    time: '1 hr ago',
    type: 'P2M Dispute — GEFU File Generation',
    amount: '₹850',
    badge: 'terminal',
    badgeLabel: 'Terminal',
  },
  {
    id: 5,
    rrn: '412905432109',
    time: '1.5 hr ago',
    type: 'P2P CB Representment — CBS Lien Removal',
    amount: '₹25,000',
    badge: 'intermediary',
    badgeLabel: 'Intermediary',
  },
  {
    id: 6,
    rrn: '412904321098',
    time: '2 hr ago',
    type: 'P2M Adjustment — Debit Consent Uploaded',
    amount: '₹3,200',
    badge: 'early',
    badgeLabel: 'Early',
  },
];

const TABS = [
  { id: 'all', label: 'All', count: 24 },
  { id: 'files', label: 'Files', count: 5 },
  { id: 'evidence', label: 'Evidence', count: 12 },
  { id: 'actions', label: 'Actions', count: 7 },
];

const TIMELINE = [
  { label: 'NPCI Report Ingested', detail: '08-04-2026 16:00 — System', done: true },
  { label: 'Merchant Mapped & Enriched', detail: '08-04-2026 16:02 — System', done: true },
  { label: 'Email Sent to Merchant', detail: '08-04-2026 16:05 — Auto', done: true },
  { label: 'Evidence Uploaded by Maker', detail: '10-04-2026 10:15 — Rajesh M.', done: true },
  { label: 'Pending Checker Approval', detail: '10-04-2026 10:42 — Awaiting', current: true },
  { label: 'Generate GEFU / CBS File', detail: '—' },
  { label: 'URCS Upload & Settlement', detail: '—' },
];

const CheckerApproval = () => {
  const [activeTab, setActiveTab] = useState('all');
  const [activeItem, setActiveItem] = useState(1);
  const [search, setSearch] = useState('');
  const [comment, setComment] = useState('');

  const filteredItems = QUEUE_ITEMS.filter((item) =>
    item.rrn.toLowerCase().includes(search.toLowerCase()) ||
    item.type.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className={styles.page}>
      <PageHeader
        title="Checker Approval Queue"
        description="Review and approve dispute actions submitted by makers"
      >
        <span className={styles.roleBadge}>Role: OPS CHECKER</span>
      </PageHeader>

      <motion.div
        className={styles.splitView}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        {/* ── Queue Panel ── */}
        <div className={styles.queuePanel}>
          <div className={styles.queueHeader}>
            <div className={styles.queueHeaderTop}>
              <h3 className={styles.queueTitle}>Pending Approvals</h3>
              <span className={styles.queueCount}>24 items awaiting review</span>
            </div>
            <div className={styles.searchWrap}>
              <Search size={14} className={styles.searchIcon} />
              <input
                className={styles.queueSearch}
                type="text"
                placeholder="Search by RRN, Merchant..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>

          <div className={styles.queueTabs}>
            {TABS.map((tab) => (
              <button
                key={tab.id}
                className={`${styles.queueTab} ${activeTab === tab.id ? styles.tabActive : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                {tab.label}
                <span className={styles.tabCount}>{tab.count}</span>
              </button>
            ))}
          </div>

          <div className={styles.queueList}>
            {filteredItems.map((item) => (
              <div
                key={item.id}
                className={`${styles.queueItem} ${activeItem === item.id ? styles.queueItemActive : ''}`}
                onClick={() => setActiveItem(item.id)}
              >
                <div className={styles.qiTop}>
                  <span className={styles.qiRrn}>{item.rrn}</span>
                  <span className={styles.qiTime}>{item.time}</span>
                </div>
                <div className={styles.qiType}>{item.type}</div>
                <div className={styles.qiMeta}>
                  <span className={styles.qiAmount}>{item.amount}</span>
                  <span className={`${styles.badge} ${styles[`badge_${item.badge}`]}`}>
                    {item.badgeLabel}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ── Detail Panel ── */}
        <div className={styles.detailPanel}>
          <div className={styles.panelTitle}>
            <span className={styles.panelDot} />
            Original Dispute Data
          </div>

          <div className={styles.dataSection}>
            <h4 className={styles.sectionHeading}>Transaction Details</h4>
            <DataRow label="RRN" value="412908765432" mono />
            <DataRow label="UTXN ID" value="UPI20260408TXNB4F2" mono />
            <DataRow label="Adj Reference" value="ADJ20260410001" mono />
            <DataRow label="Product" value="UPI" />
            <DataRow label="Type" value="P2M Chargeback" />
            <DataRow label="Amount" value="₹4,500.00" highlight />
            <DataRow label="Txn Date" value="08-04-2026 14:32 IST" />
          </div>

          <div className={styles.dataSection}>
            <h4 className={styles.sectionHeading}>Merchant / Partner</h4>
            <DataRow label="Merchant ID" value="MER0045872" mono />
            <DataRow label="Merchant Name" value="FlipMart Pvt Ltd" />
            <DataRow label="Partner ID" value="PTR00128" mono />
            <DataRow label="Account No." value="****4521" mono />
            <DataRow label="Email" value="disputes@flipmart.in" />
          </div>

          <div className={styles.dataSection}>
            <h4 className={styles.sectionHeading}>Dispute Status</h4>
            <div className={styles.dataRow}>
              <span className={styles.dataLabel}>Stage</span>
              <span className={`${styles.badge} ${styles.badge_early}`}>Early</span>
            </div>
            <DataRow label="TAT" value="2 days (SLA: 15 days)" />
            <DataRow label="Financial Impact" value="Financial" />
            <DataRow label="Bank Role" value="Remitter" />
            <div className={styles.dataRow}>
              <span className={styles.dataLabel}>Debit Consent</span>
              <span className={styles.dataValueSuccess}>Received ✓</span>
            </div>
          </div>

          <div className={styles.dataSection}>
            <h4 className={styles.sectionHeading}>Dispute Lifecycle</h4>
            <div className={styles.timeline}>
              {TIMELINE.map((step, i) => (
                <div
                  key={i}
                  className={`${styles.tlItem} ${step.done ? styles.tlDone : ''} ${step.current ? styles.tlCurrent : ''}`}
                >
                  <div className={styles.tlLabel}>{step.label}</div>
                  <div className={styles.tlDetail}>{step.detail}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── Evidence Panel ── */}
        <div className={styles.evidencePanel}>
          <div className={`${styles.panelTitle} ${styles.panelTitleGreen}`}>
            <span className={`${styles.panelDot} ${styles.panelDotGreen}`} />
            Uploaded Evidence &amp; Verification
          </div>

          <div className={`${styles.matchIndicator} ${styles.matchOk}`}>
            <CheckCircle size={14} />
            RRN and UTXN ID match between dispute record and evidence files
          </div>

          {/* Evidence File 1 */}
          <div className={styles.evidenceFile}>
            <div className={styles.efHeader}>
              <div className={styles.efName}>
                <div className={styles.efIcon}>
                  <FileText size={14} />
                </div>
                Merchant_Response_412908765432.pdf
              </div>
              <span className={styles.efSize}>245 KB</span>
            </div>
            <div className={styles.efPreview}>
              <div className={styles.efPreviewInner}>
                <FileText size={32} strokeWidth={1} className={styles.efPreviewIcon} />
                <div className={styles.efPreviewText}>Merchant response document</div>
                <div className={styles.efPreviewSub}>Uploaded by Rajesh M. on 10-04-2026</div>
              </div>
            </div>
          </div>

          {/* Evidence File 2 */}
          <div className={styles.evidenceFile}>
            <div className={styles.efHeader}>
              <div className={styles.efName}>
                <div className={`${styles.efIcon} ${styles.efIconImage}`}>
                  <Image size={14} />
                </div>
                Screenshot_TxnProof_412908765432.png
              </div>
              <span className={styles.efSize}>1.2 MB</span>
            </div>
            <div className={styles.efPreview}>
              <div className={styles.efPreviewInner}>
                <Image size={32} strokeWidth={1} className={styles.efPreviewIcon} />
                <div className={styles.efPreviewText}>Transaction screenshot evidence</div>
                <div className={styles.efPreviewSub}>Shows debit confirmation from customer app</div>
              </div>
            </div>
          </div>

          <div className={`${styles.matchIndicator} ${styles.matchWarn}`}>
            <AlertTriangle size={14} />
            Evidence code E04 — Partial documentation, merchant response pending revalidation
          </div>

          <div className={styles.dataSection} style={{ marginTop: '16px' }}>
            <h4 className={styles.sectionHeading}>Maker Submission Notes</h4>
            <div className={styles.makerNote}>
              Merchant response received via email on 10 Apr. Transaction screenshot confirms debit from
              customer account. Customer debit consent file already uploaded in batch BATCH-20260409-003.
              Recommending CB Acceptance with GEFU generation for CBS fund movement.
            </div>
          </div>

          <div className={styles.dataSection} style={{ marginTop: '16px' }}>
            <h4 className={styles.sectionHeading}>Checker Comments</h4>
            <textarea
              className={styles.commentBox}
              placeholder="Add your review comments here..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          </div>

          <div className={styles.approvalBar}>
            <button className={styles.btnOutline}>
              <ChevronLeft size={14} /> Previous
            </button>
            <button className={styles.btnOutline}>Request More Info</button>
            <button className={styles.btnRed}>✕ Reject</button>
            <button className={styles.btnGreen}>✓ Approve &amp; Generate Files</button>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

const DataRow = ({ label, value, mono, highlight }) => (
  <div className={styles.dataRow}>
    <span className={styles.dataLabel}>{label}</span>
    <span className={`${styles.dataValue} ${mono ? styles.dataValueMono : ''} ${highlight ? styles.dataValueHighlight : ''}`}>
      {value}
    </span>
  </div>
);

export default CheckerApproval;
