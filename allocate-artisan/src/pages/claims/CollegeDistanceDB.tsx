import React, { useState, useEffect } from 'react';
import { claimsApi, CollegeDistance } from '../../api/claimsApi';
import { 
  Search, 
  RefreshCw, 
  Plus, 
  Edit2, 
  Trash2, 
  Building2, 
  MapPin, 
  Hash, 
  Ruler,
  X
} from 'lucide-react';
import { Button } from '../../components/ui/button';

export default function CollegeDistanceDB() {
  const [colleges, setColleges] = useState<CollegeDistance[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCollege, setEditingCollege] = useState<CollegeDistance | null>(null);
  
  // Form state
  const [formData, setFormData] = useState({
    institutionCode: '',
    institutionName: '',
    place: '',
    distanceKm: 0,
  });

  useEffect(() => {
    loadColleges();
  }, []);

  const loadColleges = async () => {
    setLoading(true);
    try {
      const data = await claimsApi.getColleges();
      setColleges(data);
    } catch {
      console.error('Failed to load colleges');
    }
    setLoading(false);
  };

  const filtered = colleges.filter((c) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      c.institutionName?.toLowerCase().includes(q) ||
      c.institutionCode?.toLowerCase().includes(q) ||
      c.place?.toLowerCase().includes(q)
    );
  });

  const handleOpenAdd = () => {
    setEditingCollege(null);
    setFormData({ institutionCode: '', institutionName: '', place: '', distanceKm: 0 });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (college: CollegeDistance) => {
    setEditingCollege(college);
    setFormData({
      institutionCode: college.institutionCode || '',
      institutionName: college.institutionName || '',
      place: college.place || '',
      distanceKm: college.distanceKm || 0,
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this college?')) {
      try {
        await claimsApi.deleteCollege(id);
        loadColleges();
      } catch (e) {
        console.error('Failed to delete', e);
        alert('Failed to delete college.');
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.institutionName) {
      alert('Institution Name is required');
      return;
    }
    try {
      const payload: Omit<CollegeDistance, 'id'> = {
        institutionCode: formData.institutionCode || null,
        institutionName: formData.institutionName,
        place: formData.place || null,
        distanceKm: Number(formData.distanceKm),
      };
      
      if (editingCollege) {
        await claimsApi.updateCollege(editingCollege.id, payload);
      } else {
        await claimsApi.addCollege(payload);
      }
      setIsModalOpen(false);
      loadColleges();
    } catch (e) {
      console.error('Failed to save', e);
      alert('Failed to save college details.');
    }
  };

  return (
    <div className="w-full max-w-7xl mx-auto py-8 px-4 animate-in fade-in duration-500 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4 mb-2">
        <div className="flex flex-col gap-2">
          <h1 className="text-3xl font-black tracking-tight text-slate-900 mt-1">
            College Distances
          </h1>
          <div className="text-slate-500 text-sm font-medium">
            {colleges.length} institutions registered with distances from Home Institution
          </div>
        </div>
        
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="outline" onClick={loadColleges} className="gap-2 bg-white">
            <RefreshCw size={16} className="text-slate-500" /> Refresh
          </Button>
          <Button onClick={handleOpenAdd} className="gap-2 bg-indigo-600 hover:bg-indigo-700">
            <Plus size={16} /> Add College
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex flex-col justify-center items-center text-center">
          <div className="text-2xl font-black text-slate-800">{filtered.length}</div>
          <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mt-1">Showing</div>
        </div>
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex flex-col justify-center items-center text-center">
          <div className="text-2xl font-black text-slate-800">{colleges.filter(c => c.distanceKm === 0).length}</div>
          <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mt-1">Home Institution</div>
        </div>
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex flex-col justify-center items-center text-center">
          <div className="text-2xl font-black text-slate-800">{colleges.filter(c => c.distanceKm > 0).length}</div>
          <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mt-1">External</div>
        </div>
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex flex-col justify-center items-center text-center">
          <div className="text-2xl font-black text-slate-800">
            {colleges.length > 0 ? Math.max(...colleges.map(c => c.distanceKm || 0)) : 0} Km
          </div>
          <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider mt-1">Max Distance</div>
        </div>
      </div>

      {/* Search */}
      <div className="relative w-full">
        <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          className="w-full h-12 pl-11 pr-4 rounded-xl border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 outline-none transition-all shadow-sm bg-white"
          placeholder="Search by name, code, or place..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      {/* Table */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-slate-200">
          <div className="w-8 h-8 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin mb-4" />
          <div className="text-slate-500 font-bold">Loading colleges...</div>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm whitespace-nowrap">
              <thead className="bg-slate-50/80 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                <tr>
                  <th className="px-4 py-3">#</th>
                  <th className="px-4 py-3">Code</th>
                  <th className="px-4 py-3">Institution Name</th>
                  <th className="px-4 py-3">Place</th>
                  <th className="px-4 py-3 text-right">Distance (Km)</th>
                  <th className="px-4 py-3 text-right">Round Trip</th>
                  <th className="px-4 py-3 text-right">TA Amount</th>
                  <th className="px-4 py-3 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((college, index) => {
                  const roundTrip = (college.distanceKm || 0) * 2;
                  let ta = 0;
                  if (college.distanceKm > 0) {
                    if (roundTrip <= 35) {
                      ta = 150;
                    } else {
                      ta = roundTrip * 8;
                    }
                  }

                  return (
                    <tr key={college.id} className="hover:bg-slate-50 transition-colors group">
                      <td className="px-4 py-3 text-slate-500 font-medium">
                        {index + 1}
                      </td>
                      <td className="px-4 py-3">
                        {college.institutionCode ? (
                          <span className="font-mono text-xs font-bold text-slate-700 bg-slate-100 px-2 py-1 rounded">
                            {college.institutionCode}
                          </span>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <div className="max-w-[300px] truncate font-bold text-slate-800" title={college.institutionName}>
                          {college.institutionName}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-slate-600">
                        {college.place || '—'}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <span className={`font-bold ${college.distanceKm === 0 ? 'text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded' : 'text-slate-700'}`}>
                          {college.distanceKm === 0 ? 'Home (0)' : `${college.distanceKm} Km`}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-slate-600">
                        {roundTrip} Km
                      </td>
                      <td className="px-4 py-3 text-right">
                        <span className={`font-bold ${ta === 0 ? 'text-slate-400' : 'text-emerald-600'}`}>
                          {ta === 0 ? '₹0 (Internal)' : `₹${ta.toLocaleString('en-IN')}`}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-center gap-2">
                          <button
                            className="w-8 h-8 rounded flex items-center justify-center text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"
                            onClick={() => handleOpenEdit(college)}
                            title="Edit"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            className="w-8 h-8 rounded flex items-center justify-center text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                            onClick={() => handleDelete(college.id)}
                            title="Delete"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={8} className="px-4 py-12 text-center text-slate-500">
                      <Search size={32} className="mx-auto mb-3 text-slate-300" />
                      <div className="font-bold text-slate-800">No colleges found</div>
                      <div className="text-sm mt-1">We couldn't find any institutions matching your search criteria.</div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200" onClick={() => setIsModalOpen(false)}>
          <div 
            className="bg-white rounded-2xl shadow-xl w-full max-w-lg border border-slate-200 animate-in zoom-in-95 duration-200"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="p-6 border-b border-slate-100 flex justify-between items-start">
              <div className="flex items-center gap-4">
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center shrink-0 ${editingCollege ? 'bg-amber-50 text-amber-600' : 'bg-indigo-50 text-indigo-600'}`}>
                  {editingCollege ? <Edit2 size={24} /> : <Plus size={24} />}
                </div>
                <div>
                  <h3 className="text-xl font-black text-slate-900">
                    {editingCollege ? 'Edit College Details' : 'Add New Institution'}
                  </h3>
                  <div className="text-sm text-slate-500 mt-0.5">
                    {editingCollege ? 'Update the details for this institution.' : 'Register a new college into the system.'}
                  </div>
                </div>
              </div>
              <button
                className="w-8 h-8 flex items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-rose-100 hover:text-rose-600 transition-colors"
                onClick={() => setIsModalOpen(false)}
              >
                <X size={18} />
              </button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                  <Hash size={14} /> Institution Code (Optional)
                </label>
                <input
                  type="text"
                  className="w-full h-10 px-3 rounded-lg border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm font-mono"
                  placeholder="e.g. 7376"
                  value={formData.institutionCode}
                  onChange={e => setFormData({ ...formData, institutionCode: e.target.value })}
                />
              </div>

              <div>
                <label className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                  <Building2 size={14} /> Institution Name *
                </label>
                <input
                  type="text"
                  className="w-full h-10 px-3 rounded-lg border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm"
                  placeholder="Enter full college name"
                  required
                  value={formData.institutionName}
                  onChange={e => setFormData({ ...formData, institutionName: e.target.value })}
                />
              </div>

              <div>
                <label className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                  <MapPin size={14} /> Place (District/City)
                </label>
                <input
                  type="text"
                  list="tn-districts"
                  className="w-full h-10 px-3 rounded-lg border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm"
                  placeholder="Select or type a place"
                  value={formData.place}
                  onChange={e => setFormData({ ...formData, place: e.target.value })}
                />
                <datalist id="tn-districts">
                  <option value="Ariyalur" />
                  <option value="Chengalpattu" />
                  <option value="Chennai" />
                  <option value="Coimbatore" />
                  <option value="Cuddalore" />
                  <option value="Dharmapuri" />
                  <option value="Dindigul" />
                  <option value="Erode" />
                  <option value="Kallakurichi" />
                  <option value="Kanchipuram" />
                  <option value="Kanyakumari" />
                  <option value="Karur" />
                  <option value="Krishnagiri" />
                  <option value="Madurai" />
                  <option value="Mayiladuthurai" />
                  <option value="Nagapattinam" />
                  <option value="Namakkal" />
                  <option value="Nilgiris" />
                  <option value="Perambalur" />
                  <option value="Pudukkottai" />
                  <option value="Ramanathapuram" />
                  <option value="Ranipet" />
                  <option value="Salem" />
                  <option value="Sivaganga" />
                  <option value="Tenkasi" />
                  <option value="Thanjavur" />
                  <option value="Theni" />
                  <option value="Thoothukudi" />
                  <option value="Tiruchirappalli" />
                  <option value="Tirunelveli" />
                  <option value="Tirupattur" />
                  <option value="Tiruppur" />
                  <option value="Tiruvallur" />
                  <option value="Tiruvannamalai" />
                  <option value="Tiruvarur" />
                  <option value="Vellore" />
                  <option value="Viluppuram" />
                  <option value="Virudhunagar" />
                </datalist>
              </div>

              <div>
                <label className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                  <Ruler size={14} /> Distance in Km (One-way) *
                </label>
                <input
                  type="number"
                  className="w-full h-10 px-3 rounded-lg border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm font-mono"
                  required
                  min="0"
                  step="0.1"
                  value={formData.distanceKm}
                  onChange={e => setFormData({ ...formData, distanceKm: parseFloat(e.target.value) || 0 })}
                />
              </div>
              
              <div className="flex gap-3 pt-4 border-t border-slate-100">
                <Button 
                  type="button" 
                  variant="outline"
                  className="flex-1"
                  onClick={() => setIsModalOpen(false)}
                >
                  Cancel
                </Button>
                <Button 
                  type="submit" 
                  className="flex-1 bg-indigo-600 hover:bg-indigo-700"
                >
                  {editingCollege ? 'Save Changes' : 'Confirm & Add'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
