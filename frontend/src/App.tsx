import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import DashboardPage from './pages/DashboardPage';
import UploadPage from './pages/UploadPage';
import VendorsPage from './pages/VendorsPage';
import './App.css';

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-50">
        <Header />
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/upload" element={<UploadPage />} />
            <Route path="/vendors" element={<VendorsPage />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
