import {Link, Route, Routes} from 'react-router-dom';
import ServicesPage from './pages/ServicesPage';
import ServiceDetailPage from './pages/ServiceDetailPage';
import SpecDetailPage from './pages/SpecDetailPage';
import DiffsPage from './pages/DiffsPage';
import AuditPage from './pages/AuditPage';
import GroupsPage from './pages/GroupsPage';
import SettingsPage from './pages/SettingsPage';

function App() {
    return (
            <div className="min-h-screen bg-gray-50">
                {/* Skip link for accessibility */}
                <a
                        href="#main-content"
                        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-blue-600 focus:text-white focus:rounded-md"
                >
                    Skip to main content
                </a>

                <header className="bg-white shadow" role="banner">
                    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                        <div className="flex items-center justify-between">
                            <h1 className="text-3xl font-bold tracking-tight text-gray-900">
                                <Link to="/" className="hover:text-blue-600">
                                    <span aria-hidden="true">⚡</span> SpecPulse
                                </Link>
                            </h1>
                            <nav
                                    className="flex space-x-4"
                                    role="navigation"
                                    aria-label="Main navigation"
                            >
                                <Link
                                        to="/"
                                        className="text-gray-600 hover:text-gray-900"
                                        aria-label="View all services"
                                >
                                    Services
                                </Link>
                                <Link
                                        to="/groups"
                                        className="text-gray-600 hover:text-gray-900"
                                        aria-label="Manage service groups"
                                >
                                    Groups
                                </Link>
                                <Link
                                        to="/audit"
                                        className="text-gray-600 hover:text-gray-900"
                                        aria-label="View audit logs"
                                >
                                    Audit
                                </Link>
                                <Link
                                        to="/settings"
                                        className="text-gray-600 hover:text-gray-900"
                                        aria-label="Open settings"
                                >
                                    Settings
                                </Link>
                            </nav>
                        </div>
                    </div>
                </header>

                <main
                        id="main-content"
                        className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8"
                        role="main"
                >
                    <Routes>
                        <Route path="/" element={<ServicesPage/>}/>
                        <Route path="/groups" element={<GroupsPage/>}/>
                        <Route path="/settings" element={<SettingsPage/>}/>
                        <Route path="/services/:id" element={<ServiceDetailPage/>}/>
                        <Route path="/services/:id/spec" element={<SpecDetailPage/>}/>
                        <Route path="/services/:id/diffs" element={<DiffsPage/>}/>
                        <Route path="/audit" element={<AuditPage/>}/>
                    </Routes>
                </main>
            </div>
    );
}

export default App;
