import React from "react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { ShieldCheck, Menu, X } from "lucide-react";

const PublicLayout = () => {
  const [isScrolled, setIsScrolled] = React.useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = React.useState(false);
  const location = useLocation();

  React.useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 10);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const navLinks = [
    { name: "Home", path: "/" },
    { name: "About Us", path: "/about" },
    { name: "Contact", path: "/contact" },
    { name: "Pricing", path: "/pricing" },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 overflow-x-hidden font-sans">
      {/* Decorative top background */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/10 blur-[120px] rounded-full animate-pulse -z-10" />
      <div className="absolute top-[-5%] right-[-10%] w-[30%] h-[40%] bg-emerald-500/10 blur-[120px] rounded-full animate-pulse decoration-1000 -z-10" />

      {/* Navigation Bar */}
      <header
        className={`fixed top-0 w-full z-50 transition-all duration-300 ${
          isScrolled ? "bg-white/80 backdrop-blur-md shadow-sm py-3" : "bg-transparent py-5"
        }`}
      >
        <div className="container mx-auto px-4 md:px-8 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 group">
            <div className="flex h-10 w-10 items-center justify-center overflow-hidden transition-colors">
              <img src="/logo.png" alt="HallSync Logo" className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-500" />
            </div>
            <span className="text-xl font-extrabold tracking-tight text-slate-900 drop-shadow-sm">
              HallSync <span className="text-primary italic">Portal</span>
            </span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-8">
            {navLinks.map((link) => (
              <Link
                key={link.name}
                to={link.path}
                className={`text-sm font-semibold transition-colors hover:text-primary ${
                  location.pathname === link.path ? "text-primary" : "text-slate-600"
                }`}
              >
                {link.name}
              </Link>
            ))}
            <div className="flex items-center gap-4 ml-4">
              <Link to="/login">
                <Button variant="ghost" className="font-bold text-slate-700 hover:text-primary">
                  Log in
                </Button>
              </Link>
              <Link to="/pricing">
                <Button className="font-bold shadow-lg shadow-primary/20 hover:scale-105 transition-transform">
                  Get Started
                </Button>
              </Link>
            </div>
          </nav>

          {/* Mobile Menu Toggle */}
          <button
            className="md:hidden text-slate-700 p-2"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>

        {/* Mobile Navigation */}
        {mobileMenuOpen && (
          <div className="md:hidden absolute top-full left-0 w-full bg-white shadow-xl border-t border-slate-100 py-4 px-4 flex flex-col gap-4 animate-in slide-in-from-top-2">
            {navLinks.map((link) => (
              <Link
                key={link.name}
                to={link.path}
                className={`text-base font-semibold px-4 py-2 rounded-lg transition-colors ${
                  location.pathname === link.path ? "bg-primary/10 text-primary" : "text-slate-600 hover:bg-slate-50"
                }`}
                onClick={() => setMobileMenuOpen(false)}
              >
                {link.name}
              </Link>
            ))}
            <div className="h-px bg-slate-100 my-2" />
            <Link to="/login" onClick={() => setMobileMenuOpen(false)}>
              <Button variant="outline" className="w-full justify-center font-bold">
                Log in
              </Button>
            </Link>
            <Link to="/pricing" onClick={() => setMobileMenuOpen(false)}>
              <Button className="w-full justify-center font-bold">Get Started</Button>
            </Link>
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <main className="flex-grow pt-24 pb-12">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="bg-slate-900 text-slate-400 py-12 mt-auto">
        <div className="container mx-auto px-4 md:px-8 grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="md:col-span-1 space-y-4">
            <Link to="/" className="flex items-center gap-2 group opacity-90 hover:opacity-100 transition-opacity">
              <div className="flex h-8 w-8 items-center justify-center overflow-hidden transition-colors">
                <img src="/logo.png" alt="HallSync Logo" className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-500" />
              </div>
              <span className="text-xl font-extrabold tracking-tight text-white">
                HallSync <span className="text-primary italic">Portal</span>
              </span>
            </Link>
            <p className="text-sm text-slate-500 leading-relaxed">
              Secure intelligence for examination control. The ultimate SaaS solution for institutions.
            </p>
          </div>
          
          <div>
            <h3 className="text-white font-bold mb-4">Product</h3>
            <ul className="space-y-2 text-sm">
              <li><Link to="/pricing" className="hover:text-primary transition-colors">Pricing</Link></li>
              <li><Link to="/about" className="hover:text-primary transition-colors">Features</Link></li>
              <li><Link to="/contact" className="hover:text-primary transition-colors">Integrations</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-bold mb-4">Company</h3>
            <ul className="space-y-2 text-sm">
              <li><Link to="/about" className="hover:text-primary transition-colors">About Us</Link></li>
              <li><Link to="/contact" className="hover:text-primary transition-colors">Careers</Link></li>
              <li><Link to="/contact" className="hover:text-primary transition-colors">Contact</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-bold mb-4">Legal</h3>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-primary transition-colors">Privacy Policy</a></li>
              <li><a href="#" className="hover:text-primary transition-colors">Terms of Service</a></li>
            </ul>
          </div>
        </div>
        
        <div className="container mx-auto px-4 md:px-8 mt-12 pt-8 border-t border-slate-800 flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-xs text-slate-500">
            &copy; {new Date().getFullYear()} HallSync Portal. All rights reserved.
          </p>
          <div className="flex items-center gap-2 text-xs font-bold text-slate-600">
            <span>DEVELOPED BY</span>
            <span className="text-slate-400 uppercase">Varghese G T</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default PublicLayout;
