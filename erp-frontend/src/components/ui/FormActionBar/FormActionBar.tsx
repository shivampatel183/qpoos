import type { ReactNode } from "react";
import { ArrowLeft } from "lucide-react";
import "./FormActionBar.css";

export type FormActionBarProps = {
  onBack: () => void;
  backLabel?: string;
  onCancel?: () => void;
  cancelLabel?: string;
  formId?: string;
  onSave?: () => void;
  saveLabel?: string;
  saving?: boolean;
  saveDisabled?: boolean;
  children?: ReactNode;
};

export function FormActionBar({
  onBack,
  backLabel = "Back",
  onCancel,
  cancelLabel = "Cancel",
  formId,
  onSave,
  saveLabel = "Save",
  saving = false,
  saveDisabled = false,
  children,
}: FormActionBarProps) {
  return (
    <div className="form-action-bar">
      <button type="button" className="form-action-bar__back" onClick={onBack}>
        <ArrowLeft size={16} /> {backLabel}
      </button>
      <div className="form-action-bar__actions">
        {onCancel && (
          <button
            type="button"
            className="form-action-bar__cancel"
            onClick={onCancel}
            disabled={saving}
          >
            {cancelLabel}
          </button>
        )}
        <button
          type={onSave ? "button" : "submit"}
          form={onSave ? undefined : formId}
          className="form-action-bar__save"
          onClick={onSave}
          disabled={saving || saveDisabled}
        >
          {saving ? "Saving..." : saveLabel}
        </button>
        {children}
      </div>
    </div>
  );
}
