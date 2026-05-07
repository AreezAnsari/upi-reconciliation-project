import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  AlertTriangle, CheckCircle2, Clock, XCircle,
  Info, ChevronDown, Calendar, Activity, Loader2,
} from 'lucide-react';
import {
  XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  ResponsiveContainer, ComposedChart, Bar, Line,
} from 'recharts';
import { Card } from '../components/common';
import { useAuthStore } from '../store';
import { formatNumber, formatDate } from '../utils';
import { authAPI } from '../services';
import styles from './Dashboard.module.css';

// ── Per-tab data ──

const TAB_DATA = {
  upi: {
    id: 'upi',
    tabLabel: 'UPI',
    badge: '3-Way',
    headline: 'UPI DASHBOARD \u2014 3-WAY RECON (NPCI \u00d7 SWITCH \u00d7 CBS)',
    tranDate: '13-Mar-2026 (T-1)',
    stats: [
      { label: 'TOTAL TXNS (T-1)', value: '15', sub: 'NPCI: 15 | Switch: 14 | CBS: 14', accent: 'primary' },
      { label: 'AUTO-RECONCILED', value: '11', sub: '73.3% match rate (prerecon)', accent: 'success' },
      { label: 'EXCEPTIONS (UNMATCHED)', value: '4', sub: 'Across all 3 sources', accent: 'warning' },
      { label: 'FORCE MATCH ELIGIBLE', value: '3', sub: '4 exception patterns', accent: 'info' },
      { label: 'PENDING TTUM APPROVAL', value: '1', sub: 'Maker-checker pending', accent: 'gold' },
      { label: 'SETTLEMENT (NET)', value: '\u20B924,500', sub: 'Expected \u20B925,200 | \u0394 \u20B9700', accent: 'navy' },
    ],
    processId: '815065384325',
    exceptionTitle: 'Force Match \u2014 Exception Breakdown',
    exceptionCols: ['#', 'NPCI', 'SWITCH', 'CBS', 'ACTION', 'TXN COUNT', 'AMOUNT (\u20B9)'],
    exceptions: [
      { id: 1, npci: 'Y', switch: 'Y', cbs: 'Y', action: 'PLAIN KNOCK OFF', actionType: 'pko', txn: '3', amount: '6,200' },
      { id: 2, npci: 'Y', switch: 'Y', cbs: 'N', action: 'PLAIN KNOCK OFF', actionType: 'pko', txn: '2', amount: '4,800' },
      { id: 3, npci: 'Y', switch: 'N', cbs: 'N', action: 'TTUM \u2014 DR to BC', actionType: 'ttum', txn: '2', amount: '3,500' },
      { id: 4, npci: 'N', switch: 'Y', cbs: 'N', action: 'PLAIN KNOCK OFF', actionType: 'pko', txn: '1', amount: '2,800' },
      { id: 5, npci: 'Y', switch: 'N', cbs: 'Y', action: 'TTUM \u2014 CR to BC', actionType: 'ttum', txn: '1', amount: '2,400' },
      { id: 6, npci: 'N', switch: 'Y', cbs: 'Y', action: 'TTUM \u2014 DR to BC', actionType: 'ttum', txn: '1', amount: '2,100' },
      { id: 7, npci: 'N', switch: 'N', cbs: 'Y', action: 'TTUM \u2014 CR (Switch Failed)', actionType: 'ttumFail', txn: '1', amount: '1,900' },
    ],
    exceptionFooter: null,
    trendTitle: 'Daily Reconciliation Trend',
    trendDate: '13-Mar-2026',
    trendData: [
      { day: '07-Mar', matched: 10, unmatched: 3, forceMatched: 1 },
      { day: '08-Mar', matched: 11, unmatched: 2, forceMatched: 2 },
      { day: '09-Mar', matched: 9, unmatched: 4, forceMatched: 1 },
      { day: '10-Mar', matched: 12, unmatched: 2, forceMatched: 1 },
      { day: '11-Mar', matched: 10, unmatched: 3, forceMatched: 2 },
      { day: '12-Mar', matched: 13, unmatched: 1, forceMatched: 1 },
      { day: '13-Mar', matched: 11, unmatched: 4, forceMatched: 3 },
    ],
    trendLegend: ['Matched', 'Unmatched', 'Force Matched'],
    audit: {
      title: 'Force Match Audit Summary',
      runLabel: 'Run #3 Today',
      rows: [
        { icon: 'check', label: 'Total Eligible', value: '3', accent: 'success' },
        { icon: 'circle', label: 'Plain Knock Off', value: '2', accent: 'gold' },
        { icon: 'square', label: 'TTUM Entries Created', value: '1', accent: 'warning' },
        { icon: 'x', label: 'Errors', value: '0', accent: 'error' },
        { icon: 'clock', label: 'Duration', value: '1.4s', accent: 'info' },
      ],
    },
    files: {
      title: 'File Processing Status',
      items: [
        { name: 'NPCI_UPI_RECON_13MAR2026.csv', status: 'Processed', variant: 'success' },
        { name: 'CBS_UPI_EXTRACT_13MAR2026.dat', status: 'Processed', variant: 'success' },
        { name: 'SWITCH_UPI_LOG_13MAR2026.csv', status: 'Processed', variant: 'success' },
        { name: 'NEFT_MERCHANT_PAYOUT.csv', status: 'Pending', variant: 'warning' },
        { name: 'GST_MDR_CALC_13MAR.xlsx', status: 'In Progress', variant: 'info' },
      ],
      notice: 'NEFT Merchant Payout file awaited from CBS',
    },
    activity: [
      { time: '11:42', label: 'Force Match', desc: 'completed \u2014 3 txns processed, 0 errors', type: 'success' },
      { time: '11:38', label: 'Pre-Recon', desc: 'completed \u2014 11 auto-matched', type: 'success' },
      { time: '11:28', label: 'Switch File', desc: 'loaded \u2014 14 records', type: 'info' },
      { time: '10:55', label: 'NPCI File', desc: 'processed via SFTP \u2014 15 records', type: 'info' },
      { time: '10:38', label: 'CBS Extract', desc: 'loaded \u2014 14 records', type: 'gold' },
    ],
  },
  imps: {
    id: 'imps',
    tabLabel: 'IMPS',
    badge: '3-Way',
    layout: '3way',
    headline: 'IMPS DASHBOARD \u2014 3-WAY RECON (NPCI \u00d7 SWITCH \u00d7 CBS)',
    tranDate: '14-Apr-2026 (T-1)',
    stats: [
      { label: 'TOTAL TXNS (T-1)',      value: '28,450', sub: 'NPCI: 28,450 | Switch: 27,832 | CBS: 28,104', accent: 'primary' },
      { label: 'AUTO-RECONCILED',        value: '26,218', sub: '92.1% match rate (prerecon)',                  accent: 'success' },
      { label: 'EXCEPTIONS (UNMATCHED)', value: '2,232',  sub: 'Across all 3 sources',                        accent: 'warning' },
      { label: 'FORCE MATCH ELIGIBLE',   value: '1,847',  sub: '6 exception patterns',                        accent: 'info'    },
      { label: 'PENDING TTUM APPROVAL',  value: '3',      sub: 'Maker-checker pending',                       accent: 'gold'    },
      { label: 'SETTLEMENT (NET)',        value: '\u20B91,24,500', sub: 'Expected \u20B91,26,200 | \u0394 \u20B91,700', accent: 'navy' },
    ],
    processId: '815065384326',
    exceptionTitle: 'Force Match \u2014 IMPS Exception Breakdown',
    exceptionCols: ['#', 'NPCI', 'SWITCH', 'CBS', 'ACTION', 'TXN COUNT', 'AMOUNT (\u20B9)'],
    exceptions: [
      { id: 1, npci: 'Y', switch: 'Y', cbs: 'Y', action: 'PLAIN KNOCK OFF',        actionType: 'pko',      txn: '842',  amount: '38,42,600' },
      { id: 2, npci: 'Y', switch: 'Y', cbs: 'N', action: 'PLAIN KNOCK OFF',        actionType: 'pko',      txn: '618',  amount: '27,18,400' },
      { id: 3, npci: 'Y', switch: 'N', cbs: 'N', action: 'TTUM \u2014 DR to BC',  actionType: 'ttum',     txn: '387',  amount: '15,62,000' },
      { id: 4, npci: 'N', switch: 'Y', cbs: 'N', action: 'PLAIN KNOCK OFF',        actionType: 'pko',      txn: '272',  amount: '11,34,800' },
      { id: 5, npci: 'Y', switch: 'N', cbs: 'Y', action: 'TTUM \u2014 CR to BC',  actionType: 'ttum',     txn: '198',  amount: '8,22,400'  },
      { id: 6, npci: 'N', switch: 'Y', cbs: 'Y', action: 'TTUM \u2014 DR to BC',  actionType: 'ttum',     txn: '130',  amount: '5,84,200'  },
      { id: 7, npci: 'N', switch: 'N', cbs: 'Y', action: 'TTUM \u2014 CR (Switch Failed)', actionType: 'ttumFail', txn: '85', amount: '3,86,600' },
    ],
    exceptionFooter: null,
    trendTitle: 'Daily Reconciliation Trend \u2014 IMPS',
    trendDate: '14-Apr-2026',
    trendData: [
      { day: '08-Apr', matched: 24820, unmatched: 2180, forceMatched: 840 },
      { day: '09-Apr', matched: 25640, unmatched: 1960, forceMatched: 720 },
      { day: '10-Apr', matched: 23980, unmatched: 2540, forceMatched: 960 },
      { day: '11-Apr', matched: 26310, unmatched: 1720, forceMatched: 680 },
      { day: '12-Apr', matched: 25080, unmatched: 2280, forceMatched: 820 },
      { day: '13-Apr', matched: 27140, unmatched: 1480, forceMatched: 560 },
      { day: '14-Apr', matched: 26218, unmatched: 2232, forceMatched: 1847 },
    ],
    trendLegend: ['Matched', 'Unmatched', 'Force Matched'],
    audit: {
      title: 'Force Match Audit \u2014 IMPS',
      runLabel: 'Run #5 Today',
      rows: [
        { icon: 'check',  label: 'Total Eligible',        value: '1,847', accent: 'success' },
        { icon: 'circle', label: 'Plain Knock Off',        value: '1,732', accent: 'gold'    },
        { icon: 'square', label: 'TTUM Entries Created',   value: '115',   accent: 'warning' },
        { icon: 'x',      label: 'Errors',                 value: '0',     accent: 'error'   },
        { icon: 'clock',  label: 'Duration',               value: '4.2s',  accent: 'info'    },
      ],
    },
    files: {
      title: 'IMPS File Processing',
      items: [
        { name: 'NPCI_IMPS_MIS_14APR2026.csv',      status: 'Processed',   variant: 'success' },
        { name: 'CBS_IMPS_EXTRACT_14APR2026.dat',    status: 'Processed',   variant: 'success' },
        { name: 'SWITCH_IMPS_LOG_14APR2026.csv',     status: 'Processed',   variant: 'success' },
        { name: 'IMPS_MERCHANT_SETTLE_14APR.xlsx',   status: 'In Progress', variant: 'info'    },
        { name: 'NEFT_FALLBACK_IMPS_14APR.csv',      status: 'Pending',     variant: 'warning' },
      ],
      notice: null,
    },
    activity: [
      { time: '11:58', label: 'Force Match', desc: 'IMPS run #5 \u2014 1,847 eligible, 0 errors',   type: 'success' },
      { time: '11:50', label: 'Pre-Recon',   desc: 'completed \u2014 26,218 auto-matched',           type: 'success' },
      { time: '11:34', label: 'Switch File', desc: 'loaded \u2014 27,832 records via SFTP',           type: 'info'    },
      { time: '11:12', label: 'NPCI File',   desc: 'processed \u2014 28,450 records',                type: 'info'    },
      { time: '10:48', label: 'CBS Extract', desc: 'loaded \u2014 28,104 records',                   type: 'gold'    },
    ],
  },

  neft: {
    id: 'neft',
    tabLabel: 'NEFT',
    badge: '2-Way',
    layout: '2way',
    headline: 'NEFT DASHBOARD \u2014 2-WAY RECON (SFMS \u00d7 CBS)',
    tranDate: '14-Apr-2026 (T-1)',
    stats: [
      { label: 'TOTAL TXNS (T-1)',      value: '3,842',    sub: 'SFMS: 3,842 | CBS: 3,798',                    accent: 'primary' },
      { label: 'AUTO-RECONCILED',        value: '3,701',    sub: '96.3% match rate (prerecon)',                  accent: 'success' },
      { label: 'EXCEPTIONS (UNMATCHED)', value: '141',      sub: 'Across 2 sources',                            accent: 'warning' },
      { label: 'FORCE MATCH ELIGIBLE',   value: '97',       sub: '3 exception patterns',                        accent: 'info'    },
      { label: 'PENDING TTUM APPROVAL',  value: '2',        sub: 'Maker-checker pending',                       accent: 'gold'    },
      { label: 'SETTLEMENT (NET)',        value: '\u20B98,72,000', sub: 'Expected \u20B98,74,500 | \u0394 \u20B92,500', accent: 'navy' },
    ],
    processId: '815065384327',
    exceptionTitle: 'Force Match \u2014 NEFT Exception Breakdown',
    exceptionCols: ['#', 'SFMS', 'CBS', 'ACTION', 'TXN COUNT', 'AMOUNT (\u20B9)'],
    exceptions: [
      { id: 1, sfms: 'Y', cbs: 'Y', action: 'PLAIN KNOCK OFF',       actionType: 'pko',      txn: '52', amount: '4,18,200' },
      { id: 2, sfms: 'Y', cbs: 'N', action: 'TTUM \u2014 DR to BC', actionType: 'ttum',     txn: '31', amount: '2,46,800' },
      { id: 3, sfms: 'N', cbs: 'Y', action: 'TTUM \u2014 CR to BC', actionType: 'ttum',     txn: '24', amount: '1,82,400' },
      { id: 4, sfms: 'Y', cbs: 'N', action: 'PLAIN KNOCK OFF',       actionType: 'pko',      txn: '18', amount: '1,42,600' },
      { id: 5, sfms: 'N', cbs: 'Y', action: 'TTUM \u2014 DR to BC', actionType: 'ttumFail', txn: '12', amount: '94,200'   },
    ],
    exceptionFooter: {
      tables: 'REC_NEFT_SFMS_DATA | REC_NEFT_CBS_DATA',
      config: 'RCN_NEFT_CHANNEL_CONFIG',
    },
    trendTitle: 'Daily Reconciliation Trend \u2014 NEFT',
    trendDate: '14-Apr-2026',
    trendData: [
      { day: '08-Apr', matched: 3540, unmatched: 162, forceMatched: 88 },
      { day: '09-Apr', matched: 3618, unmatched: 144, forceMatched: 76 },
      { day: '10-Apr', matched: 3492, unmatched: 178, forceMatched: 102 },
      { day: '11-Apr', matched: 3724, unmatched: 128, forceMatched: 64 },
      { day: '12-Apr', matched: 3660, unmatched: 148, forceMatched: 82 },
      { day: '13-Apr', matched: 3785, unmatched: 112, forceMatched: 58 },
      { day: '14-Apr', matched: 3701, unmatched: 141, forceMatched: 97  },
    ],
    trendLegend: ['Matched', 'Unmatched', 'Force Matched'],
    audit: {
      title: 'Force Match Audit \u2014 NEFT',
      runLabel: 'Run #2 Today',
      rows: [
        { icon: 'check',  label: 'Total Eligible',       value: '97',  accent: 'success' },
        { icon: 'circle', label: 'Plain Knock Off',       value: '70',  accent: 'gold'    },
        { icon: 'square', label: 'TTUM Entries Created',  value: '27',  accent: 'warning' },
        { icon: 'x',      label: 'Errors',                value: '0',   accent: 'error'   },
        { icon: 'clock',  label: 'Duration',              value: '0.8s',accent: 'info'    },
      ],
    },
    files: {
      title: 'NEFT File Processing',
      items: [
        { name: 'SFMS_NEFT_INWARD_14APR2026.txt',    status: 'Processed',   variant: 'success' },
        { name: 'CBS_NEFT_EXTRACT_14APR2026.dat',     status: 'Processed',   variant: 'success' },
        { name: 'NEFT_RETURN_FILE_14APR2026.txt',     status: 'Processed',   variant: 'success' },
        { name: 'NEFT_OUTWARD_SETTLE_14APR.csv',      status: 'In Progress', variant: 'info'    },
      ],
      notice: '44 unmatched transactions pending SFMS acknowledgement',
    },
    activity: [
      { time: '12:05', label: 'Force Match',   desc: 'NEFT run #2 \u2014 97 eligible, 0 errors',       type: 'success' },
      { time: '11:58', label: 'Pre-Recon',     desc: 'completed \u2014 3,701 auto-matched',             type: 'success' },
      { time: '11:40', label: 'CBS Extract',   desc: 'loaded \u2014 3,798 records',                     type: 'gold'    },
      { time: '11:22', label: 'SFMS Inward',   desc: 'processed via SFTP \u2014 3,842 records',         type: 'info'    },
      { time: '10:58', label: 'Return File',   desc: 'NEFT return loaded \u2014 12 reversal entries',   type: 'info'    },
    ],
  },

  rtgs: {
    id: 'rtgs',
    tabLabel: 'RTGS',
    badge: '2-Way',
    layout: '2way',
    headline: 'RTGS DASHBOARD \u2014 2-WAY RECON (SFMS \u00d7 CBS)',
    tranDate: '14-Apr-2026 (T-1)',
    stats: [
      { label: 'TOTAL TXNS (T-1)',      value: '524',      sub: 'SFMS: 524 | CBS: 521',                         accent: 'primary' },
      { label: 'AUTO-RECONCILED',        value: '516',      sub: '98.5% match rate (prerecon)',                   accent: 'success' },
      { label: 'EXCEPTIONS (UNMATCHED)', value: '8',        sub: 'Across 2 sources',                             accent: 'warning' },
      { label: 'FORCE MATCH ELIGIBLE',   value: '5',        sub: '2 exception patterns',                         accent: 'info'    },
      { label: 'PENDING TTUM APPROVAL',  value: '1',        sub: 'Maker-checker pending',                        accent: 'gold'    },
      { label: 'SETTLEMENT (NET)',        value: '\u20B942,50,000', sub: 'Expected \u20B942,53,200 | \u0394 \u20B93,200', accent: 'navy' },
    ],
    processId: '815065384328',
    exceptionTitle: 'Force Match \u2014 RTGS Exception Breakdown',
    exceptionCols: ['#', 'SFMS', 'CBS', 'ACTION', 'TXN COUNT', 'AMOUNT (\u20B9)'],
    exceptions: [
      { id: 1, sfms: 'Y', cbs: 'Y', action: 'PLAIN KNOCK OFF',       actionType: 'pko',      txn: '3', amount: '22,50,000' },
      { id: 2, sfms: 'Y', cbs: 'N', action: 'TTUM \u2014 DR to BC', actionType: 'ttum',     txn: '2', amount: '14,80,000' },
      { id: 3, sfms: 'N', cbs: 'Y', action: 'TTUM \u2014 CR to BC', actionType: 'ttumFail', txn: '1', amount: '8,20,000'  },
    ],
    exceptionFooter: {
      tables: 'REC_RTGS_SFMS_DATA | REC_RTGS_CBS_DATA',
      config: 'RCN_RTGS_CHANNEL_CONFIG',
    },
    trendTitle: 'Daily Reconciliation Trend \u2014 RTGS',
    trendDate: '14-Apr-2026',
    trendData: [
      { day: '08-Apr', matched: 498, unmatched: 12, forceMatched: 4 },
      { day: '09-Apr', matched: 512, unmatched: 8,  forceMatched: 3 },
      { day: '10-Apr', matched: 487, unmatched: 15, forceMatched: 6 },
      { day: '11-Apr', matched: 520, unmatched: 6,  forceMatched: 2 },
      { day: '12-Apr', matched: 508, unmatched: 10, forceMatched: 4 },
      { day: '13-Apr', matched: 519, unmatched: 7,  forceMatched: 3 },
      { day: '14-Apr', matched: 516, unmatched: 8,  forceMatched: 5 },
    ],
    trendLegend: ['Matched', 'Unmatched', 'Force Matched'],
    audit: {
      title: 'Force Match Audit \u2014 RTGS',
      runLabel: 'Run #1 Today',
      rows: [
        { icon: 'check',  label: 'Total Eligible',       value: '5',   accent: 'success' },
        { icon: 'circle', label: 'Plain Knock Off',       value: '3',   accent: 'gold'    },
        { icon: 'square', label: 'TTUM Entries Created',  value: '2',   accent: 'warning' },
        { icon: 'x',      label: 'Errors',                value: '0',   accent: 'error'   },
        { icon: 'clock',  label: 'Duration',              value: '0.3s',accent: 'info'    },
      ],
    },
    files: {
      title: 'RTGS File Processing',
      items: [
        { name: 'SFMS_RTGS_INWARD_14APR2026.txt',    status: 'Processed',   variant: 'success' },
        { name: 'CBS_RTGS_EXTRACT_14APR2026.dat',     status: 'Processed',   variant: 'success' },
        { name: 'RTGS_RETURN_FILE_14APR2026.txt',     status: 'Processed',   variant: 'success' },
        { name: 'RTGS_OUTWARD_SETTLE_14APR.csv',      status: 'In Progress', variant: 'info'    },
      ],
      notice: '2 high-value transactions pending manual SFMS confirmation',
    },
    activity: [
      { time: '12:18', label: 'Force Match', desc: 'RTGS run #1 \u2014 5 eligible, 0 errors',         type: 'success' },
      { time: '12:10', label: 'Pre-Recon',   desc: 'completed \u2014 516 auto-matched',                type: 'success' },
      { time: '11:55', label: 'CBS Extract', desc: 'loaded \u2014 521 records (high-value)',            type: 'gold'    },
      { time: '11:38', label: 'SFMS Inward', desc: 'processed via SFTP \u2014 524 records',            type: 'info'    },
      { time: '11:20', label: 'Return File', desc: 'RTGS return loaded \u2014 3 reversal entries',     type: 'info'    },
    ],
  },

  pps_wallet: {
    id: 'pps_wallet',
    tabLabel: 'PPS vs Wallet',
    badge: '2-Way',
    layout: 'addmoney',
    counterLabel: 'Wallet',
    headline: 'ADD MONEY DASHBOARD \u2014 2-WAY RECON (PPS \u00d7 WALLET)',
    tranDate: '14-Apr-2026 (T-1)',
    stats: [
      { label: 'TOTAL TXNS (T-1)',      value: '6,374',  sub: 'UPI: 4,106 | PG: 1,580 | Sweep: 688', accent: 'primary' },
      { label: 'AUTO-RECONCILED',        value: '8',      sub: '0.1% match rate (prerecon)',           accent: 'success' },
      { label: 'EXCEPTIONS (UNMATCHED)', value: '6,366',  sub: 'Across all 3 sources',                accent: 'warning' },
      { label: 'FORCE MATCH ELIGIBLE',   value: '10',     sub: 'RB response code transactions',        accent: 'info'    },
      { label: 'PENDING TTUM APPROVAL',  value: '0',      sub: 'Maker-checker pending',                accent: 'gold'    },
      { label: 'SETTLEMENT (NET)',        value: '\u20B90', sub: 'Expected \u20B90 | \u0394 \u20B90', accent: 'navy'    },
    ],
    processId: '815065384335',
    exceptionTitle: 'Force Match \u2014 Exception Breakdown',
    exceptionCols: ['#', 'PPS', 'WALLET', 'ACTION', 'TXN COUNT', 'AMOUNT (\u20B9)'],
    exceptions: [
      { id: 1, pps: 'Y', counter: 'Y', action: 'PLAIN KNOCK OFF',  actionType: 'pko',  txn: '3', amount: '6,200' },
      { id: 2, pps: 'Y', counter: 'N', action: 'PLAIN KNOCK OFF',  actionType: 'pko',  txn: '2', amount: '4,800' },
      { id: 3, pps: 'Y', counter: 'N', action: 'TTUM \u2014 DR to BC', actionType: 'ttum', txn: '2', amount: '3,500' },
      { id: 4, pps: 'N', counter: 'Y', action: 'PLAIN KNOCK OFF',  actionType: 'pko',  txn: '1', amount: '2,800' },
      { id: 5, pps: 'N', counter: 'Y', action: 'RECON ONLY',       actionType: 'recon',txn: '2', amount: '1,200' },
    ],
    exceptionFooter: null,
    trendTitle: 'Daily Reconciliation Trend',
    trendDate: 'Apr-2026',
    trendData: [
      { day: '01-Apr', matched: 6, unmatched: 3, forceMatched: 1 },
      { day: '02-Apr', matched: 4, unmatched: 5, forceMatched: 2 },
      { day: '03-Apr', matched: 8, unmatched: 2, forceMatched: 1 },
      { day: '04-Apr', matched: 5, unmatched: 4, forceMatched: 2 },
      { day: '05-Apr', matched: 10,unmatched: 2, forceMatched: 1 },
      { day: '06-Apr', matched: 7, unmatched: 3, forceMatched: 2 },
      { day: '07-Apr', matched: 8, unmatched: 3, forceMatched: 1 },
    ],
    trendLegend: ['Matched', 'Unmatched', 'Force Matched'],
    detailRows: [
      { source: 'UPI',             sourceCls: 'upi', ppsCount: 4106, ppsAmt: 14800000, counterCount: 4020, counterAmt: 14500000, matchedCount: 3950, matchedAmt: 14200000, unmatchedCount: 156, unmatchedAmt: 600000,  status: 'partial' },
      { source: 'Payment Gateway', sourceCls: 'pg',  ppsCount: 1580, ppsAmt: 6400000,  counterCount: 1560, counterAmt: 6320000,  matchedCount: 1540, matchedAmt: 6200000,  unmatchedCount: 40,  unmatchedAmt: 200000,  status: 'matched' },
      { source: 'Sweep-In',        sourceCls: 'sw',  ppsCount: 688,  ppsAmt: 3100000,  counterCount: 670,  counterAmt: 3020000,  matchedCount: 640,  matchedAmt: 2880000,  unmatchedCount: 48,  unmatchedAmt: 220000,  status: 'partial' },
    ],
    rules: [
      { title: 'UPI', icon: 'upi', primary: 'Match on UTR/RRN and Amount.', forceMatch: 'Allow when UTR matches with timestamp diff (±5 min).', color: '#2563eb' },
      { title: 'PG (Cards / Net Banking)', icon: 'pg', primary: 'Match on Txn ID / Order ID and Amount.', forceMatch: 'Allow when auth code + amount match, ref number differs.', color: '#7c3aed' },
      { title: 'Sweep-In', icon: 'sweep', primary: 'Match on Account, Transfer Ref, Amount.', forceMatch: 'Allow when amount + account match, ref missing.', color: 'var(--color-accent-500)' },
    ],
  },

  pps_cbs: {
    id: 'pps_cbs',
    tabLabel: 'PPS vs CBS',
    badge: '2-Way',
    layout: 'addmoney',
    counterLabel: 'CBS',
    headline: 'ADD MONEY DASHBOARD \u2014 2-WAY RECON (PPS \u00d7 CBS)',
    tranDate: '14-Apr-2026 (T-1)',
    stats: [
      { label: 'TOTAL TXNS (T-1)',      value: '6,374',   sub: 'UPI: 4,106 | PG: 1,580 | Sweep: 688', accent: 'primary' },
      { label: 'AUTO-RECONCILED',        value: '5,890',   sub: '92.4% match rate (prerecon)',          accent: 'success' },
      { label: 'EXCEPTIONS (UNMATCHED)', value: '484',     sub: 'Across all 3 sources',                accent: 'warning' },
      { label: 'FORCE MATCH ELIGIBLE',   value: '23',      sub: 'FAILED status transactions',           accent: 'info'    },
      { label: 'PENDING TTUM APPROVAL',  value: '5',       sub: 'Maker-checker pending',                accent: 'gold'    },
      { label: 'SETTLEMENT (NET)',        value: '\u20B92.4 Cr', sub: 'Expected \u20B92.5 Cr | \u0394 \u20B98.2L', accent: 'navy' },
    ],
    processId: '815065384336',
    exceptionTitle: 'Force Match \u2014 Exception Breakdown',
    exceptionCols: ['#', 'PPS', 'CBS', 'ACTION', 'TXN COUNT', 'AMOUNT (\u20B9)'],
    exceptions: [
      { id: 1, pps: 'Y', counter: 'Y', action: 'PLAIN KNOCK OFF',       actionType: 'pko',  txn: '3',  amount: '6,200' },
      { id: 2, pps: 'Y', counter: 'N', action: 'PLAIN KNOCK OFF',       actionType: 'pko',  txn: '5',  amount: '18,900' },
      { id: 3, pps: 'N', counter: 'Y', action: 'TTUM \u2014 FAILED→DR/CR', actionType: 'ttum', txn: '12', amount: '85,400' },
      { id: 4, pps: 'N', counter: 'Y', action: 'RECON ONLY',            actionType: 'recon',txn: '3',  amount: '12,600' },
    ],
    exceptionFooter: null,
    trendTitle: 'Daily Reconciliation Trend',
    trendDate: 'Apr-2026',
    trendData: [
      { day: '01-Apr', matched: 5580, unmatched: 480, forceMatched: 20 },
      { day: '02-Apr', matched: 5620, unmatched: 440, forceMatched: 18 },
      { day: '03-Apr', matched: 5540, unmatched: 510, forceMatched: 24 },
      { day: '04-Apr', matched: 5700, unmatched: 390, forceMatched: 15 },
      { day: '05-Apr', matched: 5760, unmatched: 360, forceMatched: 12 },
      { day: '06-Apr', matched: 5810, unmatched: 330, forceMatched: 10 },
      { day: '07-Apr', matched: 5890, unmatched: 484, forceMatched: 23 },
    ],
    trendLegend: ['Matched', 'Unmatched', 'Force Matched'],
    detailRows: [
      { source: 'UPI',             sourceCls: 'upi', ppsCount: 4106, ppsAmt: 14800000, counterCount: 4090, counterAmt: 14750000, matchedCount: 4080, matchedAmt: 14700000, unmatchedCount: 26,  unmatchedAmt: 100000, status: 'matched' },
      { source: 'Payment Gateway', sourceCls: 'pg',  ppsCount: 1580, ppsAmt: 6400000,  counterCount: 1575, counterAmt: 6380000,  matchedCount: 1570, matchedAmt: 6350000,  unmatchedCount: 10,  unmatchedAmt: 50000,  status: 'matched' },
      { source: 'Sweep-In',        sourceCls: 'sw',  ppsCount: 688,  ppsAmt: 3100000,  counterCount: 680,  counterAmt: 3070000,  matchedCount: 665,  matchedAmt: 2990000,  unmatchedCount: 23,  unmatchedAmt: 110000, status: 'partial' },
    ],
    rules: [
      { title: 'UPI', icon: 'upi', primary: 'Match on UTR/RRN and Amount.', forceMatch: 'Allow when UTR matches with timestamp diff (±5 min).', color: '#2563eb' },
      { title: 'PG (Cards / Net Banking)', icon: 'pg', primary: 'Match on Txn ID / Order ID and Amount.', forceMatch: 'Allow when auth code + amount match, ref number differs.', color: '#7c3aed' },
      { title: 'Sweep-In', icon: 'sweep', primary: 'Match on Account, Transfer Ref, Amount.', forceMatch: 'Allow when amount + account match, ref missing.', color: 'var(--color-accent-500)' },
    ],
  },

  aeps: {
    id: 'aeps',
    tabLabel: 'AEPS',
    badge: '3-Way',
    headline: 'AEPS DASHBOARD \u2014 3-WAY RECON (NPCI \u00d7 SWITCH \u00d7 CBS) \u2014 ACQ CASH WITHDRAWAL',
    tranDate: '13-Mar-2026 (T-1) | TRAN_CODE=04 | FILE_NAME LIKE ACQ%',
    stats: [
      { label: 'NPCI TXNS (REC_AEPS)', value: '13', sub: 'ACQ Cash W/D only (04/ACQ%)', accent: 'primary' },
      { label: 'CBS (REC_UPHST)', value: '10', sub: 'Matched in prerecon', accent: 'success' },
      { label: 'SWITCH (REC_AEPSWT)', value: '12', sub: 'DD-MON-RR timestamp format', accent: 'info' },
      { label: 'EXCEPTIONS', value: '3', sub: 'Force match eligible', accent: 'warning' },
      { label: 'TTUM PENDING APPROVAL', value: '1', sub: 'Maker-checker queue', accent: 'gold' },
      { label: 'SETTLEMENT (NET)', value: '\u20B922,800', sub: 'Expected \u20B923,500 | \u0394 \u20B9700', accent: 'navy' },
    ],
    processId: '815065384329',
    exceptionTitle: 'Force Match \u2014 AEPS Exception Patterns',
    exceptionCols: ['#', 'NPCI', 'CBS', 'SWITCH', 'ACTION', 'TXN COUNT', 'AMOUNT (\u20B9)'],
    exceptions: [
      { id: 1, npci: 'Y', switch: 'Y', cbs: 'Y', action: 'PLAIN KNOCK OFF', actionType: 'pko', txn: '2', amount: '5,400' },
      { id: 2, npci: 'Y', switch: 'N', cbs: 'Y', action: 'PLAIN KNOCK OFF', actionType: 'pko', txn: '1', amount: '3,200' },
      { id: 3, npci: 'N', switch: 'N', cbs: 'Y', action: 'TTUM \u2014 DR to BC (customer_Acct)', actionType: 'ttum', txn: '1', amount: '2,800' },
      { id: 4, npci: 'N', switch: 'Y', cbs: 'N', action: 'PLAIN KNOCK OFF', actionType: 'pko', txn: '1', amount: '2,400' },
      { id: 5, npci: 'Y', switch: 'Y', cbs: 'N', action: 'TTUM \u2014 CR to BC (channel_Acct)', actionType: 'ttum', txn: '1', amount: '2,100' },
      { id: 6, npci: 'N', switch: 'Y', cbs: 'Y', action: 'TTUM \u2014 DR to BC (channel_Acct)', actionType: 'ttum', txn: '1', amount: '1,900' },
      { id: 7, npci: 'Y', switch: 'N', cbs: 'N', action: 'TTUM \u2014 CR (Switch Failed Lookup)', actionType: 'ttumFail', txn: '1', amount: '1,800' },
    ],
    exceptionFooter: {
      tables: 'REC_AEPS_AEOF_DATA | REC_UPHST_AEOF_DATA | REC_AEPSWT_AEOF_DATA',
      config: 'RCN_AEPS_CHANNEL_CONFIG',
    },
    trendTitle: 'Daily Reconciliation Trend \u2014 AEPS ACQ',
    trendDate: 'Last 7 Days',
    trendData: [
      { day: '07-Mar', autoReconciled: 9, forceMatched: 2, openExceptions: 1 },
      { day: '08-Mar', autoReconciled: 10, forceMatched: 1, openExceptions: 2 },
      { day: '09-Mar', autoReconciled: 8, forceMatched: 2, openExceptions: 2 },
      { day: '10-Mar', autoReconciled: 11, forceMatched: 1, openExceptions: 1 },
      { day: '11-Mar', autoReconciled: 9, forceMatched: 2, openExceptions: 1 },
      { day: '12-Mar', autoReconciled: 10, forceMatched: 1, openExceptions: 2 },
      { day: '13-Mar', autoReconciled: 10, forceMatched: 3, openExceptions: 1 },
    ],
    trendLegend: ['Auto-Reconciled', 'Force Matched', 'Open Exceptions'],
    audit: {
      title: 'Force Match Audit \u2014 AEPS',
      runLabel: 'Run #2 Today',
      rows: [
        { icon: 'check', label: 'Total Eligible', value: '3', accent: 'success' },
        { icon: 'circle', label: 'Plain Knock Off', value: '2', accent: 'gold' },
        { icon: 'square', label: 'TTUM Entries Created', value: '1', accent: 'warning' },
        { icon: 'checkDouble', label: 'Channel Acct Resolved', value: '1', accent: 'info' },
        { icon: 'x', label: 'Errors', value: '0', accent: 'error' },
        { icon: 'clock', label: 'Duration', value: '2.8s', accent: 'info' },
      ],
    },
    files: {
      title: 'AEPS File Processing',
      items: [
        { name: 'ACQ_04_NPCI_MIS_13MAR2026.pgp', sub: 'Decrypted via SFTP pipeline', status: 'Processed', variant: 'success' },
        { name: 'CBS_UPHST_AEPS_EXTRACT.dat', sub: '12 records loaded', status: 'Processed', variant: 'success' },
        { name: 'AEPSWT_SWITCH_LOG.csv', sub: 'DD-MON-RR format auto-detected', status: 'Processed', variant: 'success' },
        { name: 'BC_CHANNEL_CONFIG.csv', sub: '18 channels mapped', status: 'Updated', variant: 'info' },
      ],
      notice: '1 NPCI-only txn routed via Switch Failed Lookup',
    },
    activity: [
      { time: '11:35', label: 'Force Match', desc: 'AEPS run #2 \u2014 3 txns, 7 patterns, 0 errors', type: 'success' },
      { time: '11:30', label: 'Channel Config', desc: '1 BC account resolved via RCN_AEPS_CHANNEL_CONFIG', type: 'info' },
      { time: '11:22', label: 'Pre-Recon', desc: 'completed \u2014 dyn_pidflag1 set to Y on 10 rows', type: 'success' },
      { time: '11:05', label: 'SFTP Pipeline', desc: 'ACQ_04 PGP file decrypted \u2192 13 records extracted', type: 'error' },
      { time: '10:40', label: 'SQL*Loader', desc: 'Switch file loaded \u2014 12 records, DD-MON-RR format', type: 'gold' },
    ],
  },
};

// ── Chart colors per tab ──

const CHART_COLORS = {
  upi: {
    bar1: { key: 'matched',     name: 'Matched',       fill: '#234b73' },
    bar2: { key: 'unmatched',   name: 'Unmatched',     fill: '#f59e0b' },
    bar3: { key: 'forceMatched',name: 'Force Matched', fill: '#9ca3af' },
  },
  imps: {
    bar1: { key: 'matched',     name: 'Matched',       fill: '#17a398' },
    bar2: { key: 'unmatched',   name: 'Unmatched',     fill: '#f59e0b' },
    bar3: { key: 'forceMatched',name: 'Force Matched', fill: '#9ca3af' },
  },
  neft: {
    bar1: { key: 'matched',     name: 'Matched',       fill: '#7C3AED' },
    bar2: { key: 'unmatched',   name: 'Unmatched',     fill: '#f59e0b' },
    bar3: { key: 'forceMatched',name: 'Force Matched', fill: '#9ca3af' },
  },
  rtgs: {
    bar1: { key: 'matched',     name: 'Matched',       fill: '#F97316' },
    bar2: { key: 'unmatched',   name: 'Unmatched',     fill: '#ef4444' },
    bar3: { key: 'forceMatched',name: 'Force Matched', fill: '#9ca3af' },
  },
  aeps: {
    bar1: { key: 'autoReconciled', name: 'Auto-Reconciled',  fill: '#10b981' },
    bar2: { key: 'forceMatched',   name: 'Force Matched',    fill: '#f59e0b' },
    bar3: { key: 'openExceptions', name: 'Open Exceptions',  fill: '#ef4444' },
  },
  pps_wallet: {
    bar1: { key: 'matched',     name: 'Matched',       fill: '#17a398' },
    bar2: { key: 'unmatched',   name: 'Unmatched',     fill: '#3b82f6' },
    bar3: { key: 'forceMatched',name: 'Force Matched', fill: '#9ca3af' },
  },
  pps_cbs: {
    bar1: { key: 'matched',     name: 'Matched',       fill: '#1a8a7d' },
    bar2: { key: 'unmatched',   name: 'Unmatched',     fill: '#3b82f6' },
    bar3: { key: 'forceMatched',name: 'Force Matched', fill: '#9ca3af' },
  },
};

// ── Audit icon map ──

const AUDIT_ICONS = {
  check: <CheckCircle2 size={16} />,
  circle: <span className={styles?.auditDot} style={{ borderColor: 'var(--color-gold-500)' }} />,
  square: <span className={styles?.auditSquare} />,
  checkDouble: <CheckCircle2 size={16} />,
  x: <XCircle size={16} />,
  clock: <Clock size={16} />,
};

// ── Animation ──

const fadeUp = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.35, ease: [0.25, 0.46, 0.45, 0.94] } },
};

const stagger = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.05 } },
};

// ── Custom chart tooltip ──

const ChartTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className={styles.chartTooltip}>
      <p className={styles.tooltipLabel}>{label}</p>
      {payload.map((entry) => (
        <div key={entry.dataKey} className={styles.tooltipRow}>
          <span className={styles.tooltipDot} style={{ background: entry.color }} />
          <span className={styles.tooltipName}>{entry.name}</span>
          <span className={styles.tooltipVal}>{formatNumber(entry.value)}</span>
        </div>
      ))}
    </div>
  );
};

// ── Y/N flag cell ──

const FlagCell = ({ value }) => (
  <span className={value === 'Y' ? styles.flagY : styles.flagN}>{value}</span>
);

// ── Action badge ──

const ActionBadge = ({ label, type }) => {
  const cls = type === 'pko'      ? styles.actionPko
    : type === 'ttumFail'         ? styles.actionTtumFail
    : type === 'recon'            ? styles.actionRecon
    : styles.actionTtum;
  return <span className={`${styles.actionTag} ${cls}`}>{label}</span>;
};

// ── Status pill ──

const StatusPill = ({ status, variant }) => {
  const cls = variant === 'success' ? styles.badge_success
    : variant === 'warning' ? styles.badge_warning
    : variant === 'error' ? styles.badge_error
    : styles.badge_info;
  return <span className={`${styles.statusBadge} ${cls}`}>{status}</span>;
};

// ── Source label map for UPI file-wise breakdown ──
const UPI_SOURCE_LABEL = {
  REC_UPI_MIS_UBEN_DATA:    'NPCI',
  REC_UPHST_UPI_UBEN_DATA:  'CBS',
  REC_UPISWT_SWT_UBEN_DATA: 'Switch',
};

const fmtCur = (n) =>
  '₹' + new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 }).format(n ?? 0);

// Compact currency (Cr / L / plain) for the source-wise detail table
const fmtAmt = (n) => {
  if (n >= 1e7) return '₹' + (n / 1e7).toFixed(2) + ' Cr';
  if (n >= 1e5) return '₹' + (n / 1e5).toFixed(1) + ' L';
  return '₹' + new Intl.NumberFormat('en-IN').format(n);
};

// ── Main Dashboard ──

const Dashboard = () => {
  const { user, token } = useAuthStore();
  const [activeTab, setActiveTab] = useState('upi');

  // ── UPI recon KPI from API ──
  const [upiKpi,     setUpiKpi]     = useState(null);
  const [upiLoading, setUpiLoading] = useState(false);

  useEffect(() => {
    if (!token) return;
    setUpiLoading(true);
    authAPI.getReconDashboard(token)
      .then((res) => {
        if (res.status === 'SUCCESS' && res.data?.length > 0) {
          setUpiKpi(res.data[0]);
        }
      })
      .catch(() => {})
      .finally(() => setUpiLoading(false));
  }, [token]);

  // ── Build live UPI stats from API data (falls back to TAB_DATA defaults) ──
  const buildUpiStats = (kpiRaw) => {
    if (!kpiRaw) return TAB_DATA.upi.stats;          // show defaults while loading
    const kpi  = kpiRaw.kpiSummary ?? {};
    const files = kpi.fileWiseCount ?? [];

    // Build "NPCI: X | Switch: X | CBS: X" ordered sub-line
    const sourceOrder = ['NPCI', 'Switch', 'CBS'];
    const sourceMap = {};
    files.forEach((f) => {
      const label = UPI_SOURCE_LABEL[f.dataTableName];
      if (label) sourceMap[label] = formatNumber(f.dataCount);
    });
    const fileSubLine = sourceOrder
      .filter((s) => sourceMap[s] !== undefined)
      .map((s) => `${s}: ${sourceMap[s]}`)
      .join(' | ') || 'Across all sources';

    const matchRate   = kpi.matchRatePercent != null
      ? `${kpi.matchRatePercent.toFixed(1)}% match rate (prerecon)`
      : TAB_DATA.upi.stats[1].sub;

    const settlementSub = kpi.settlementExpected != null
      ? `Expected ${fmtCur(kpi.settlementExpected)} | Δ ${fmtCur(Math.abs(kpi.settlementDelta ?? 0))}`
      : TAB_DATA.upi.stats[5].sub;

    return [
      { label: 'TOTAL TXNS (T-1)',        value: formatNumber(kpi.totalTxns ?? 0),           sub: fileSubLine,                                     accent: 'primary' },
      { label: 'AUTO-RECONCILED',          value: formatNumber(kpi.autoReconciled ?? 0),       sub: matchRate,                                       accent: 'success' },
      { label: 'EXCEPTIONS (UNMATCHED)',   value: formatNumber(kpi.exceptions ?? 0),           sub: kpi.exceptionsNote    ?? 'Across all 3 sources', accent: 'warning' },
      { label: 'FORCE MATCH ELIGIBLE',     value: formatNumber(kpi.forceMatchEligible ?? 0),   sub: kpi.forceMatchNote    ?? 'N/A',                  accent: 'info'    },
      { label: 'PENDING TTUM APPROVAL',    value: formatNumber(kpi.pendingTtumApproval ?? 0),  sub: kpi.pendingTtumNote   ?? 'Maker-checker pending', accent: 'gold'   },
      { label: 'SETTLEMENT (NET)',          value: fmtCur(kpi.settlementNet ?? 0),             sub: settlementSub,                                   accent: 'navy'    },
    ];
  };

  // ── Derive tranDate from API (yyyy-MM-dd → dd-MM-yyyy) ──
  const upiTranDate = upiKpi?.tranDate
    ? formatDate(new Date(upiKpi.tranDate))
    : TAB_DATA.upi.tranDate;

  const tab = {
    ...TAB_DATA[activeTab],
    ...(activeTab === 'upi' ? {
      stats:    buildUpiStats(upiKpi),
      tranDate: upiTranDate,
    } : {}),
  };
  const chartCfg = CHART_COLORS[activeTab];

  return (
    <div className={styles.dashboard}>
      {/* ── Welcome Card ── */}
      <motion.div className={styles.greeting} initial={{ opacity: 0, x: -12 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.4 }}>
        <div className={styles.greetingLeft}>
          <div className={styles.greetingIcon}>
            <Activity size={20} />
          </div>
          <div>
            <h1 className={styles.greetingTitle}>Welcome back{user?.name ? `, ${user.name}` : ''}</h1>
            <p className={styles.greetingSub}>Here's your reconciliation overview for today</p>
          </div>
        </div>
        <div className={styles.greetingRight}>
          <span className={styles.liveDot} />
          <span className={styles.liveText}>Live</span>
        </div>
      </motion.div>

      {/* ── Tab Bar ── */}
      <div className={styles.tabBar}>
        {Object.values(TAB_DATA).map((t) => (
          <button
            key={t.id}
            className={`${styles.tab} ${activeTab === t.id ? styles.tabActive : ''}`}
            onClick={() => setActiveTab(t.id)}
          >
            {t.tabLabel}
            <span className={styles.tabBadge}>{t.badge}</span>
          </button>
        ))}
      </div>

      <AnimatePresence mode="wait">
        <motion.div
          key={activeTab}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.25 }}
          className={styles.tabContent}
        >
          {/* ── Headline Row ── */}
          <div className={styles.headlineRow}>
            <p className={styles.headline}>{tab.headline}</p>
            <div className={styles.tranDate}>
              <Calendar size={14} />
              <span>Tran Date: <strong>{tab.tranDate}</strong></span>
            </div>
          </div>

          {/* ── Stats Row ── */}
          <motion.div className={styles.statsRow} variants={stagger} initial="hidden" animate="show">
            {tab.stats.map((s) => (
              <motion.div key={s.label} variants={fadeUp}>
                <div className={`${styles.statCard} ${styles[`stat_${s.accent}`]}`}>
                  <p className={styles.statLabel}>
                    {s.label}
                    {activeTab === 'upi' && upiLoading && (
                      <Loader2 size={10} className={styles.statLoader} />
                    )}
                  </p>
                  <p className={styles.statValue}>{s.value}</p>
                  {s.sub && <p className={styles.statSub}>{s.sub}</p>}
                </div>
              </motion.div>
            ))}
          </motion.div>

          {/* ── Middle Row: Exception Table + Trend Chart ── */}
          <div className={styles.middleRow}>
            <motion.div variants={fadeUp} initial="hidden" animate="show">
              <Card className={styles.panel} padding="none" animate={false}>
                <div className={styles.panelHeader}>
                  <h3 className={styles.panelTitle}>{tab.exceptionTitle}</h3>
                  <span className={styles.processBadge}>Process: {tab.processId}</span>
                </div>
                <div className={styles.tableScroll}>
                  <table className={styles.exTable}>
                    <thead>
                      <tr>
                        {tab.exceptionCols.map((col) => (
                          <th key={col}>{col}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {tab.exceptions.map((row) => (
                        <tr key={row.id}>
                          <td className={styles.rowNum}>{row.id}</td>
                          {tab.layout === 'addmoney' ? (
                            // PPS vs Wallet / CBS — PPS | Counter
                            <>
                              <td><FlagCell value={row.pps} /></td>
                              <td><FlagCell value={row.counter} /></td>
                            </>
                          ) : tab.layout === '2way' ? (
                            // NEFT / RTGS — SFMS | CBS
                            <>
                              <td><FlagCell value={row.sfms} /></td>
                              <td><FlagCell value={row.cbs} /></td>
                            </>
                          ) : activeTab === 'aeps' ? (
                            // AEPS — NPCI | CBS | SWITCH
                            <>
                              <td><FlagCell value={row.npci} /></td>
                              <td><FlagCell value={row.cbs} /></td>
                              <td><FlagCell value={row.switch} /></td>
                            </>
                          ) : (
                            // UPI / IMPS — NPCI | SWITCH | CBS
                            <>
                              <td><FlagCell value={row.npci} /></td>
                              <td><FlagCell value={row.switch} /></td>
                              <td><FlagCell value={row.cbs} /></td>
                            </>
                          )}
                          <td><ActionBadge label={row.action} type={row.actionType} /></td>
                          <td className={styles.numCell}>{row.txn}</td>
                          <td className={styles.numCell}>{row.amount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {tab.exceptionFooter && (
                  <div className={styles.exFooter}>
                    <span>Tables: <strong>{tab.exceptionFooter.tables}</strong></span>
                    <span>Channel Config: <strong>{tab.exceptionFooter.config}</strong></span>
                  </div>
                )}
              </Card>
            </motion.div>

            <motion.div variants={fadeUp} initial="hidden" animate="show" transition={{ delay: 0.08 }}>
              <Card className={styles.panel} padding="none" animate={false}>
                <div className={styles.panelHeader}>
                  <h3 className={styles.panelTitle}>{tab.trendTitle}</h3>
                  <span className={styles.trendDateBadge}>{tab.trendDate}</span>
                </div>
                <div className={styles.chartWrapper}>
                  <ResponsiveContainer width="100%" height={280}>
                    <ComposedChart data={tab.trendData} margin={{ top: 8, right: 16, bottom: 4, left: -12 }}>
                      <defs>
                        <linearGradient id={`grad_${activeTab}`} x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor={chartCfg.bar1.fill} stopOpacity={0.9} />
                          <stop offset="100%" stopColor={chartCfg.bar1.fill} stopOpacity={0.5} />
                        </linearGradient>
                        <linearGradient id="gradLine" x1="0" y1="0" x2="1" y2="0">
                          <stop offset="0%" stopColor="#17a398" />
                          <stop offset="100%" stopColor="#2ebfb3" />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" vertical={false} />
                      <XAxis dataKey="day" tick={{ fontSize: 11, fill: '#9ca3af', fontFamily: 'DM Sans' }}
                        axisLine={false} tickLine={false} dy={6} />
                      <YAxis tick={{ fontSize: 11, fill: '#9ca3af', fontFamily: 'DM Sans' }}
                        axisLine={false} tickLine={false} allowDecimals={false} />
                      <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(23,163,152,0.04)' }} />
                      <Legend iconType="circle" iconSize={8}
                        wrapperStyle={{ fontSize: 12, fontFamily: 'DM Sans', paddingTop: 12, color: '#6b7280' }} />
                      <Bar dataKey={chartCfg.bar1.key} name={chartCfg.bar1.name}
                        fill={`url(#grad_${activeTab})`} radius={[3, 3, 0, 0]} barSize={18} />
                      <Bar dataKey={chartCfg.bar2.key} name={chartCfg.bar2.name}
                        fill={chartCfg.bar2.fill} radius={[3, 3, 0, 0]} barSize={18} opacity={0.85} />
                      <Bar dataKey={chartCfg.bar3.key} name={chartCfg.bar3.name}
                        fill={chartCfg.bar3.fill} radius={[3, 3, 0, 0]} barSize={18} opacity={0.7} />
                      <Line dataKey={chartCfg.bar1.key} name={`${chartCfg.bar1.name} Trend`}
                        stroke="url(#gradLine)" strokeWidth={2.5}
                        dot={{ r: 4, fill: '#17a398', stroke: '#fff', strokeWidth: 2 }}
                        activeDot={{ r: 6, fill: '#17a398', stroke: '#fff', strokeWidth: 2 }} />
                    </ComposedChart>
                  </ResponsiveContainer>
                </div>
              </Card>
            </motion.div>
          </div>

          {/* ── Bottom Row ── */}
          {tab.layout === 'addmoney' ? (
            <>
              {/* Source-Wise Detail Table */}
              <motion.div variants={fadeUp} initial="hidden" animate="show">
                <Card className={styles.panel} padding="none" animate={false}>
                  <div className={styles.panelHeader}>
                    <h3 className={styles.panelTitle}>Reconciliation Status — Source-Wise</h3>
                    <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                      <button className={styles.dlBtn}>CSV</button>
                      <button className={styles.dlBtn}>Excel</button>
                    </div>
                  </div>
                  <div className={styles.tableScroll}>
                    <table className={styles.detailTable}>
                      <thead>
                        <tr>
                          <th>Source</th>
                          <th>PPS Count</th><th>PPS Amt (₹)</th>
                          <th>{tab.counterLabel} Count</th><th>{tab.counterLabel} Amt (₹)</th>
                          <th>Matched</th><th>Matched Amt</th>
                          <th>Unmatched</th><th>Unmatched Amt</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {tab.detailRows.map((r) => (
                          <tr key={r.source}>
                            <td><span className={`${styles.sourceChip} ${styles[`sourceChip_${r.sourceCls}`]}`}>{r.source}</span></td>
                            <td className={styles.monoCell}>{formatNumber(r.ppsCount)}</td>
                            <td className={styles.monoCell}>{fmtAmt(r.ppsAmt)}</td>
                            <td className={styles.monoCell}>{formatNumber(r.counterCount)}</td>
                            <td className={styles.monoCell}>{fmtAmt(r.counterAmt)}</td>
                            <td className={`${styles.monoCell} ${styles.cellGreen}`}>{formatNumber(r.matchedCount)}</td>
                            <td className={`${styles.monoCell} ${styles.cellGreen}`}>{fmtAmt(r.matchedAmt)}</td>
                            <td className={`${styles.monoCell} ${styles.cellRed}`}>{formatNumber(r.unmatchedCount)}</td>
                            <td className={`${styles.monoCell} ${styles.cellRed}`}>{fmtAmt(r.unmatchedAmt)}</td>
                            <td><span className={`${styles.statusDot} ${r.status === 'matched' ? styles.statusDotGreen : styles.statusDotAmber}`}>{r.status === 'matched' ? 'Matched' : 'Partial'}</span></td>
                          </tr>
                        ))}
                      </tbody>
                      <tfoot>
                        {(() => {
                          const t = tab.detailRows.reduce((a, r) => ({
                            ppsCount: a.ppsCount + r.ppsCount, ppsAmt: a.ppsAmt + r.ppsAmt,
                            counterCount: a.counterCount + r.counterCount, counterAmt: a.counterAmt + r.counterAmt,
                            matchedCount: a.matchedCount + r.matchedCount, matchedAmt: a.matchedAmt + r.matchedAmt,
                            unmatchedCount: a.unmatchedCount + r.unmatchedCount, unmatchedAmt: a.unmatchedAmt + r.unmatchedAmt,
                          }), { ppsCount:0, ppsAmt:0, counterCount:0, counterAmt:0, matchedCount:0, matchedAmt:0, unmatchedCount:0, unmatchedAmt:0 });
                          const rate = ((t.matchedCount / t.ppsCount) * 100).toFixed(1);
                          return (
                            <tr className={styles.detailFooter}>
                              <td><strong>Total</strong></td>
                              <td className={styles.monoCell}><strong>{formatNumber(t.ppsCount)}</strong></td>
                              <td className={styles.monoCell}><strong>{fmtAmt(t.ppsAmt)}</strong></td>
                              <td className={styles.monoCell}><strong>{formatNumber(t.counterCount)}</strong></td>
                              <td className={styles.monoCell}><strong>{fmtAmt(t.counterAmt)}</strong></td>
                              <td className={`${styles.monoCell} ${styles.cellGreen}`}><strong>{formatNumber(t.matchedCount)}</strong></td>
                              <td className={`${styles.monoCell} ${styles.cellGreen}`}><strong>{fmtAmt(t.matchedAmt)}</strong></td>
                              <td className={`${styles.monoCell} ${styles.cellRed}`}><strong>{formatNumber(t.unmatchedCount)}</strong></td>
                              <td className={`${styles.monoCell} ${styles.cellRed}`}><strong>{fmtAmt(t.unmatchedAmt)}</strong></td>
                              <td><span className={`${styles.statusDot} ${parseFloat(rate) >= 95 ? styles.statusDotGreen : styles.statusDotAmber}`}><strong>{parseFloat(rate) >= 95 ? 'Matched' : 'Partial'} ({rate}%)</strong></span></td>
                            </tr>
                          );
                        })()}
                      </tfoot>
                    </table>
                  </div>
                </Card>
              </motion.div>

              {/* Force Match Rule Definitions */}
              <motion.div variants={fadeUp} initial="hidden" animate="show" transition={{ delay: 0.06 }}>
                <div className={styles.rulesTitle}>Force Match Rule Definitions</div>
                <div className={styles.rulesGrid}>
                  {tab.rules.map((rule, i) => (
                    <div key={rule.title} className={`${styles.ruleCard} ${styles[`ruleCard_${i + 1}`]}`}>
                      <div className={styles.ruleHeader}>
                        <span className={styles.ruleIcon} style={{ color: rule.color }}>
                          {rule.icon === 'upi'   && '📱'}
                          {rule.icon === 'pg'    && '💳'}
                          {rule.icon === 'sweep' && '🔄'}
                        </span>
                        {rule.title}
                      </div>
                      <div className={styles.ruleItem}>
                        <div className={`${styles.ruleItemIcon} ${styles.ruleItemIconGreen}`}>✓</div>
                        <div>
                          <div className={styles.ruleItemType}>Primary</div>
                          <div className={styles.ruleItemText}>{rule.primary}</div>
                        </div>
                      </div>
                      <div className={styles.ruleItem}>
                        <div className={`${styles.ruleItemIcon} ${styles.ruleItemIconAmber}`}>⚡</div>
                        <div>
                          <div className={styles.ruleItemType}>Force Match</div>
                          <div className={styles.ruleItemText}>{rule.forceMatch}</div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </motion.div>
            </>
          ) : (
          <div className={styles.bottomRow}>
            {/* Audit Summary */}
            <motion.div variants={fadeUp} initial="hidden" animate="show">
              <Card className={styles.panel} padding="none" animate={false}>
                <div className={styles.panelHeader}>
                  <h3 className={styles.panelTitle}>{tab.audit.title}</h3>
                  <span className={styles.runBadge}>{tab.audit.runLabel}</span>
                </div>
                <div className={styles.auditList}>
                  {tab.audit.rows.map((r) => (
                    <div key={r.label} className={styles.auditItem}>
                      <span className={`${styles.auditIcon} ${styles[`auditIcon_${r.accent}`]}`}>
                        {r.icon === 'check' && <CheckCircle2 size={15} />}
                        {r.icon === 'circle' && <span className={styles.auditCircle} />}
                        {r.icon === 'square' && <span className={styles.auditSquare} />}
                        {r.icon === 'checkDouble' && <CheckCircle2 size={15} />}
                        {r.icon === 'x' && <XCircle size={15} />}
                        {r.icon === 'clock' && <Clock size={15} />}
                      </span>
                      <span className={styles.auditLabel}>{r.label}</span>
                      <span className={`${styles.auditValue} ${styles[`text_${r.accent}`]}`}>{r.value}</span>
                    </div>
                  ))}
                </div>
              </Card>
            </motion.div>

            {/* File Processing */}
            <motion.div variants={fadeUp} initial="hidden" animate="show" transition={{ delay: 0.06 }}>
              <Card className={styles.panel} padding="none" animate={false}>
                <div className={styles.panelHeader}>
                  <h3 className={styles.panelTitle}>{tab.files.title}</h3>
                </div>
                <div className={styles.fileList}>
                  {tab.files.items.map((f) => (
                    <div key={f.name} className={styles.fileItem}>
                      <div className={styles.fileInfo}>
                        <span className={styles.fileName}>{f.name}</span>
                        {f.sub && <span className={styles.fileSub}>{f.sub}</span>}
                      </div>
                      <StatusPill status={f.status} variant={f.variant} />
                    </div>
                  ))}
                  {tab.files.notice && (
                    <div className={styles.fileNotice}>
                      <Info size={14} />
                      <span>{tab.files.notice}</span>
                    </div>
                  )}
                </div>
              </Card>
            </motion.div>

            {/* Activity Log */}
            <motion.div variants={fadeUp} initial="hidden" animate="show" transition={{ delay: 0.1 }}>
              <Card className={styles.panel} padding="none" animate={false}>
                <div className={styles.panelHeader}>
                  <h3 className={styles.panelTitle}>Activity Log</h3>
                </div>
                <div className={styles.activityList}>
                  {tab.activity.map((a, i) => (
                    <div key={i} className={styles.activityItem}>
                      <span className={styles.activityTime}>{a.time}</span>
                      <span className={`${styles.activityDot} ${styles[`actDot_${a.type}`]}`} />
                      <div className={styles.activityText}>
                        <strong>{a.label}</strong> {a.desc}
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </motion.div>
          </div>
          )}
        </motion.div>
      </AnimatePresence>
    </div>
  );
};

export default Dashboard;
