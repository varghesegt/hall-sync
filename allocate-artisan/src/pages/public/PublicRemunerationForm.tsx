import React, { useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { toast } from 'sonner';
import { FileSignature, ChevronRight, ChevronLeft, CheckCircle2 } from 'lucide-react';
import { NetworkOfflineOverlay } from '@/components/ui/NetworkOfflineOverlay';

export default function PublicRemunerationForm() {
  const { batchId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [step, setStep] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);

  // Form State: Step 1
  const [staffData, setStaffData] = useState({
    staffName: '',
    postHeld: 'EXAMINER',
    mobileNo: '',
    designation: '',
    facultyType: 'INTERNAL',
    institutionName: '',
    bankAccountNumber: '',
    ifscCode: '',
    bankName: '',
    branch: '',
    sessionsAttended: 'FN & AN',
    isGovernmentHoliday: false
  });

  // Form State: Step 2
  type SubjectDetail = {
    code: string;
    fnScripts: string;
    anScripts: string;
    showNextPrompt: boolean;
    hasNext: boolean;
  };

  const [subjects, setSubjects] = useState<SubjectDetail[]>([
    { code: '', fnScripts: '0', anScripts: '0', showNextPrompt: true, hasNext: false }
  ]);

  const handleStaffChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setStaffData({ ...staffData, [e.target.name]: e.target.value });
  };

  const validateSubjectCode = (code: string) => {
    const regex = /^[A-Z0-9]+$/;
    return regex.test(code);
  };

  const updateSubject = (index: number, field: keyof SubjectDetail, value: any) => {
    const updated = [...subjects];
    updated[index] = { ...updated[index], [field]: value };
    setSubjects(updated);
  };

  const handleNextPromptChange = (index: number, hasNext: boolean) => {
    const updated = [...subjects];
    updated[index].hasNext = hasNext;

    if (hasNext) {
      if (updated.length === index + 1 && updated.length < 10) {
        updated.push({ code: '', fnScripts: '0', anScripts: '0', showNextPrompt: true, hasNext: false });
      }
    } else {
      updated.splice(index + 1);
    }
    
    setSubjects(updated);
  };

  const submitForm = async () => {
    // Validate subject codes
    for (let i = 0; i < subjects.length; i++) {
      if (!validateSubjectCode(subjects[i].code)) {
        toast.error(`Subject ${i + 1} Code is invalid. Please enter in CAPITAL LETTERS and numbers only (e.g., CGB1222).`);
        return;
      }
    }

    setIsSubmitting(true);
    try {
      const payload = {
        ...staffData,
        examSeason: searchParams.get('season') || '',
        valuationDate: searchParams.get('date') || '',
        subjects: subjects.map(s => ({
          code: s.code,
          fnScripts: parseInt(s.fnScripts) || 0,
          anScripts: parseInt(s.anScripts) || 0
        }))
      };

      const API_URL = import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || '/api/v1';
      const response = await fetch(`${API_URL}/public/claims/${batchId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        setIsSubmitted(true);
      } else {
        const error = await response.json();
        toast.error(error.message || 'Failed to submit the form.');
      }
    } catch (err) {
      toast.error('Network error. Failed to submit.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSubmitted) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-2xl shadow-sm border border-emerald-100 p-8 text-center animate-in zoom-in-95 duration-500">
          <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <CheckCircle2 size={32} />
          </div>
          <h1 className="text-2xl font-black text-slate-900 mb-2">Form Submitted</h1>
          <p className="text-slate-500 mb-8">
            Thank you! Your remuneration details have been successfully submitted for processing.
          </p>
          <Button onClick={() => window.location.reload()} variant="outline" className="w-full">
            Submit Another Form
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 pb-12">
      <NetworkOfflineOverlay />
      
      <header className="bg-indigo-600 px-4 py-6 shadow-sm sticky top-0 z-10">
        <div className="max-w-3xl mx-auto flex items-center gap-3">
          <div className="bg-white/20 p-2 rounded-lg text-white backdrop-blur-sm">
            <FileSignature size={24} />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">Remuneration Claim Form</h1>
            <p className="text-indigo-200 text-xs mt-0.5">End Semester Examinations</p>
          </div>
        </div>
      </header>

      <main className="max-w-3xl mx-auto p-4 mt-6">
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
          
          {/* Progress Bar */}
          <div className="flex bg-slate-100 border-b border-slate-200">
            <div className={`flex-1 py-3 text-center text-sm font-bold ${step === 1 ? 'bg-white text-indigo-600 border-b-2 border-indigo-600' : 'text-slate-500'}`}>
              Step 1: Staff Details
            </div>
            <div className={`flex-1 py-3 text-center text-sm font-bold ${step === 2 ? 'bg-white text-indigo-600 border-b-2 border-indigo-600' : 'text-slate-500'}`}>
              Step 2: Valuation Details
            </div>
          </div>

          <div className="p-6 md:p-8">
            
            {/* STEP 1: STAFF DETAILS */}
            {step === 1 && (
              <div className="space-y-6 animate-in slide-in-from-left-4 duration-300">
                <h2 className="text-lg font-bold text-slate-800 border-b pb-2">Staff Information</h2>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Full Name *</Label>
                    <Input name="staffName" value={staffData.staffName} onChange={handleStaffChange} placeholder="e.g. Dr. John Doe" required />
                  </div>
                  <div className="space-y-1">
                    <Label>Mobile Number *</Label>
                    <Input name="mobileNo" value={staffData.mobileNo} onChange={handleStaffChange} placeholder="10-digit number" required />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Designation</Label>
                    <Input name="designation" value={staffData.designation} onChange={handleStaffChange} placeholder="e.g. Assistant Professor" />
                  </div>
                  <div className="space-y-1">
                    <Label>Institution Name</Label>
                    <Input name="institutionName" value={staffData.institutionName} onChange={handleStaffChange} placeholder="College Name" />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Post Held</Label>
                    <select name="postHeld" value={staffData.postHeld} onChange={handleStaffChange} className="w-full h-10 px-3 border rounded-md bg-white text-sm">
                      <option value="EXAMINER">Examiner</option>
                      <option value="ASSISTANT EXAMINER">Assistant Examiner</option>
                      <option value="CHIEF EXAMINER">Chief Examiner</option>
                    </select>
                  </div>
                  <div className="space-y-1">
                    <Label>Faculty Type</Label>
                    <select name="facultyType" value={staffData.facultyType} onChange={handleStaffChange} className="w-full h-10 px-3 border rounded-md bg-white text-sm">
                      <option value="INTERNAL">Internal</option>
                      <option value="EXTERNAL">External</option>
                    </select>
                  </div>
                </div>

                <h2 className="text-lg font-bold text-slate-800 border-b pb-2 pt-4">Bank Details (For NEFT)</h2>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Account Number</Label>
                    <Input name="bankAccountNumber" value={staffData.bankAccountNumber} onChange={handleStaffChange} placeholder="Enter A/C Number" />
                  </div>
                  <div className="space-y-1">
                    <Label>IFSC Code</Label>
                    <Input name="ifscCode" value={staffData.ifscCode} onChange={handleStaffChange} placeholder="e.g. SBIN0001234" />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Bank Name</Label>
                    <Input name="bankName" value={staffData.bankName} onChange={handleStaffChange} placeholder="e.g. State Bank of India" />
                  </div>
                  <div className="space-y-1">
                    <Label>Branch</Label>
                    <Input name="branch" value={staffData.branch} onChange={handleStaffChange} placeholder="Branch Name" />
                  </div>
                </div>

                <div className="flex justify-end pt-4">
                  <Button 
                    onClick={() => {
                      if (!staffData.staffName || !staffData.mobileNo) {
                        toast.error("Please fill in Name and Mobile Number");
                        return;
                      }
                      setStep(2);
                    }} 
                    className="gap-2 bg-indigo-600 hover:bg-indigo-700"
                  >
                    Next Step <ChevronRight size={16} />
                  </Button>
                </div>
              </div>
            )}

            {/* STEP 2: VALUATION DETAILS */}
            {step === 2 && (
              <div className="space-y-8 animate-in slide-in-from-right-4 duration-300">
                
                <div className="bg-indigo-50 border border-indigo-100 p-4 rounded-xl text-indigo-800 text-sm">
                  <strong>Important:</strong> Enter subject codes in <strong>CAPITAL LETTERS</strong> only (e.g. CGB1222).
                </div>

                {subjects.map((subject, index) => (
                  <div key={index} className="p-4 border border-slate-200 rounded-xl bg-slate-50 space-y-4 shadow-sm relative">
                    <h3 className="font-bold text-slate-800 border-b border-slate-200 pb-2">
                      📝 Subject {index + 1} Details {index === 0 ? '(Required)' : '(Optional)'}
                    </h3>
                    
                    <div className="space-y-2">
                      <Label>Subject {index + 1} Code</Label>
                      <Input 
                        value={subject.code} 
                        onChange={(e) => updateSubject(index, 'code', e.target.value.toUpperCase())} 
                        placeholder="e.g. CGB1222" 
                        required={index === 0}
                      />
                      {subject.code && !validateSubjectCode(subject.code) && (
                        <p className="text-xs text-rose-500 font-medium">Please enter in CAPITAL LETTERS and numbers only (e.g., CGB1222).</p>
                      )}
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label className="text-xs">No. of Scripts (FN)</Label>
                        <Input 
                          type="number" 
                          min="0"
                          value={subject.fnScripts} 
                          onChange={(e) => updateSubject(index, 'fnScripts', e.target.value)} 
                        />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-xs">No. of Scripts (AN)</Label>
                        <Input 
                          type="number" 
                          min="0"
                          value={subject.anScripts} 
                          onChange={(e) => updateSubject(index, 'anScripts', e.target.value)} 
                        />
                      </div>
                    </div>

                    {index < 9 && subject.showNextPrompt && (
                      <div className="pt-4 mt-2 border-t border-slate-200">
                        <Label className="text-sm font-semibold block mb-3">Do you have another subject to add?</Label>
                        <div className="flex gap-4">
                          <label className="flex items-center gap-2 cursor-pointer bg-white border px-3 py-2 rounded-lg flex-1 hover:bg-slate-50">
                            <input 
                              type="radio" 
                              name={`hasNext-${index}`} 
                              checked={subject.hasNext === true}
                              onChange={() => handleNextPromptChange(index, true)}
                              className="w-4 h-4 text-indigo-600"
                            />
                            <span className="text-sm font-medium">Yes</span>
                          </label>
                          <label className="flex items-center gap-2 cursor-pointer bg-white border px-3 py-2 rounded-lg flex-1 hover:bg-slate-50">
                            <input 
                              type="radio" 
                              name={`hasNext-${index}`} 
                              checked={subject.hasNext === false}
                              onChange={() => handleNextPromptChange(index, false)}
                              className="w-4 h-4 text-indigo-600"
                            />
                            <span className="text-sm font-medium">No</span>
                          </label>
                        </div>
                      </div>
                    )}
                  </div>
                ))}

                <div className="flex justify-between pt-4 border-t">
                  <Button onClick={() => setStep(1)} variant="outline" className="gap-2">
                    <ChevronLeft size={16} /> Back
                  </Button>
                  
                  {(!subjects[subjects.length - 1].hasNext || subjects.length === 10) && (
                    <Button 
                      onClick={submitForm} 
                      disabled={isSubmitting || !subjects[0].code || !validateSubjectCode(subjects[0].code)}
                      className="bg-emerald-600 hover:bg-emerald-700 text-white gap-2"
                    >
                      {isSubmitting ? 'Submitting...' : 'Submit Form'}
                    </Button>
                  )}
                </div>
              </div>
            )}
            
          </div>
        </div>
      </main>
    </div>
  );
}
