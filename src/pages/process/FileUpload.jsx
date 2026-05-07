import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Upload, FileText, X, CheckCircle, AlertCircle, Cloud, FolderOpen, File } from 'lucide-react';
import { Button, Select, Card } from '../../components/common';
import { formatNumber } from '../../utils/helpers';
import styles from './Process.module.css';

const FileUpload = () => {
  const [selectedProcessType, setSelectedProcessType] = useState('');
  const [selectedFileType, setSelectedFileType] = useState('');
  const [files, setFiles] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [uploadProgress, setUploadProgress] = useState({});
  const fileInputRef = useRef(null);

  const processTypes = [
    { value: '', label: 'Select Process Type' },
    { value: 'debitcard', label: 'Debit Card' },
    { value: 'creditcard', label: 'Credit Card' },
    { value: 'upi', label: 'UPI' },
    { value: 'neft', label: 'NEFT' },
    { value: 'rtgs', label: 'RTGS' },
    { value: 'imps', label: 'IMPS' }
  ];

  const fileTypes = [
    { value: '', label: 'Select File Type' },
    { value: 'pos_raw', label: 'POS Raw' },
    { value: 'cbs_raw', label: 'CBS Raw' },
    { value: 'switch_raw', label: 'Switch Raw' },
    { value: 'network_raw', label: 'Network Raw' }
  ];

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFiles = Array.from(e.dataTransfer.files);
    addFiles(droppedFiles);
  };

  const handleFileSelect = (e) => {
    const selectedFiles = Array.from(e.target.files);
    addFiles(selectedFiles);
  };

  const addFiles = (newFiles) => {
    const fileObjs = newFiles.map((file, index) => ({
      id: `file-${Date.now()}-${index}`,
      file,
      name: file.name,
      size: file.size,
      type: file.type,
      status: 'pending'
    }));
    setFiles(prev => [...prev, ...fileObjs]);
  };

  const removeFile = (fileId) => {
    setFiles(prev => prev.filter(f => f.id !== fileId));
    setUploadProgress(prev => {
      const newProgress = { ...prev };
      delete newProgress[fileId];
      return newProgress;
    });
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const handleUpload = async () => {
    if (files.length === 0 || !selectedProcessType || !selectedFileType) return;

    for (const fileObj of files) {
      setUploadProgress(prev => ({ ...prev, [fileObj.id]: 0 }));
      setFiles(prev => prev.map(f => 
        f.id === fileObj.id ? { ...f, status: 'uploading' } : f
      ));

      // Simulate upload progress
      for (let progress = 0; progress <= 100; progress += 10) {
        await new Promise(resolve => setTimeout(resolve, 100));
        setUploadProgress(prev => ({ ...prev, [fileObj.id]: progress }));
      }

      setFiles(prev => prev.map(f => 
        f.id === fileObj.id ? { ...f, status: 'completed' } : f
      ));
    }
  };

  const getFileIcon = (status) => {
    switch (status) {
      case 'completed': return <CheckCircle size={20} className={styles.iconSuccess} />;
      case 'error': return <AlertCircle size={20} className={styles.iconError} />;
      case 'uploading': return <Cloud size={20} className={styles.iconProcessing} />;
      default: return <FileText size={20} className={styles.iconPending} />;
    }
  };

  return (
    <div className={styles.pageContainer}>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        {/* Page Header */}
        <div className={styles.pageHeader}>
          <div className={styles.headerContent}>
            <div className={styles.headerIcon}>
              <Upload size={24} />
            </div>
            <div>
              <h1 className={styles.pageTitle}>File Upload</h1>
              <p className={styles.pageSubtitle}>Upload transaction files for processing</p>
            </div>
          </div>
        </div>

        {/* Configuration */}
        <Card className={styles.configCard}>
          <Card.Header>
            <Card.Title>Upload Configuration</Card.Title>
            <Card.Description>Select process type and file category</Card.Description>
          </Card.Header>
          <Card.Content>
            <div className={styles.uploadConfigGrid}>
              <Select
                label="Process Type"
                options={processTypes}
                value={selectedProcessType}
                onChange={(e) => setSelectedProcessType(e.target.value)}
              />
              <Select
                label="File Type"
                options={fileTypes}
                value={selectedFileType}
                onChange={(e) => setSelectedFileType(e.target.value)}
              />
            </div>
          </Card.Content>
        </Card>

        {/* Upload Zone */}
        <Card className={styles.uploadCard}>
          <Card.Content>
            <div
              className={`${styles.dropZone} ${isDragging ? styles.dropZoneActive : ''}`}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
            >
              <input
                ref={fileInputRef}
                type="file"
                multiple
                onChange={handleFileSelect}
                style={{ display: 'none' }}
                accept=".csv,.xlsx,.xls,.txt"
              />
              <div className={styles.dropZoneContent}>
                <div className={styles.uploadIconWrapper}>
                  <FolderOpen size={48} />
                </div>
                <h3>Drag & drop files here</h3>
                <p>or click to browse</p>
                <span className={styles.fileFormats}>
                  Supported formats: CSV, XLSX, XLS, TXT
                </span>
              </div>
            </div>
          </Card.Content>
        </Card>

        {/* Selected Files */}
        {files.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
          >
            <Card className={styles.filesCard}>
              <Card.Header>
                <div className={styles.filesHeader}>
                  <div>
                    <Card.Title>Selected Files</Card.Title>
                    <Card.Description>{files.length} file(s) ready for upload</Card.Description>
                  </div>
                  <Button onClick={handleUpload} disabled={!selectedProcessType || !selectedFileType}>
                    <Upload size={18} />
                    Upload All
                  </Button>
                </div>
              </Card.Header>
              <Card.Content>
                <AnimatePresence>
                  {files.map((fileObj) => (
                    <motion.div
                      key={fileObj.id}
                      className={styles.uploadFileItem}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: 20 }}
                    >
                      <div className={styles.fileIcon}>
                        {getFileIcon(fileObj.status)}
                      </div>
                      <div className={styles.fileDetails}>
                        <span className={styles.fileName}>{fileObj.name}</span>
                        <span className={styles.fileMeta}>{formatFileSize(fileObj.size)}</span>
                        {fileObj.status === 'uploading' && (
                          <div className={styles.uploadProgressMini}>
                            <div 
                              className={styles.uploadProgressFill}
                              style={{ width: `${uploadProgress[fileObj.id] || 0}%` }}
                            />
                          </div>
                        )}
                      </div>
                      <div className={styles.fileActions}>
                        {fileObj.status === 'completed' && (
                          <span className={styles.completedBadge}>Uploaded</span>
                        )}
                        {fileObj.status !== 'uploading' && (
                          <button 
                            className={styles.removeBtn}
                            onClick={() => removeFile(fileObj.id)}
                          >
                            <X size={18} />
                          </button>
                        )}
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>
              </Card.Content>
            </Card>
          </motion.div>
        )}
      </motion.div>
    </div>
  );
};

export default FileUpload;
