import React from 'react';
import { Modal } from './Modal';
import { AlertTriangle } from 'lucide-react';

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDestructive?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen, onClose, onConfirm, title, message, confirmText = 'Confirm', cancelText = 'Cancel', isDestructive = false
}) => {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title}>
      <div className="flex items-start gap-4 mb-6">
        {isDestructive && (
          <div className="bg-red-500/10 p-3 rounded-full text-red-500">
            <AlertTriangle className="w-6 h-6" />
          </div>
        )}
        <p className="text-gray-300 py-2">{message}</p>
      </div>
      <div className="flex justify-end gap-3">
        <button onClick={onClose} className="btn-secondary">
          {cancelText}
        </button>
        <button onClick={() => { onConfirm(); onClose(); }} className={isDestructive ? 'btn-danger' : 'btn-primary'}>
          {confirmText}
        </button>
      </div>
    </Modal>
  );
};
