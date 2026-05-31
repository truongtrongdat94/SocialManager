import { useState, useEffect } from 'react';
import { Button, Card, Input } from '../../components';
import { api } from '../../lib/api';

interface SocialAccount {
  id: string;
  accountName: string;
  platform: string;
}

interface AutoPilotConfig {
  id: string;
  socialAccountId: string;
  socialAccountName: string;
  platform: string;
  keywords: string[];
  frequencyHours: number;
  status: 'ACTIVE' | 'PAUSED' | 'STOPPED';
  lastRunAt: string | null;
  nextRunAt: string | null;
  promptTemplate: string | null;
  createdAt: string;
  updatedAt: string;
}

export function AutoPilot() {
  const [configs, setConfigs] = useState<AutoPilotConfig[]>([]);
  const [accounts, setAccounts] = useState<SocialAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [formData, setFormData] = useState({
    socialAccountId: '',
    keywords: '',
    frequencyHours: 6,
    status: 'ACTIVE' as const,
    promptTemplate: '',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [configsRes, accountsRes] = await Promise.all([
        api.get('/api/autopilot'),
        api.get('/api/social-accounts'),
      ]);
      
      // Handle ApiResponse format: { data: [...], success: true, message: "..." }
      const configsData = configsRes.data.data || configsRes.data;
      const accountsData = accountsRes.data.data || accountsRes.data;
      
      setConfigs(configsData);
      setAccounts(accountsData);
    } catch (error) {
      console.error('Error loading data:', error);
      alert('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      const keywordsArray = formData.keywords
        .split(',')
        .map(k => k.trim())
        .filter(k => k.length > 0);

      if (keywordsArray.length === 0) {
        alert('Please enter at least one keyword');
        return;
      }

      await api.post('/api/autopilot', {
        socialAccountId: formData.socialAccountId,
        keywords: keywordsArray,
        frequencyHours: formData.frequencyHours,
        status: formData.status,
        promptTemplate: formData.promptTemplate || null,
      });

      alert('Auto Pilot config created successfully!');
      setShowCreateForm(false);
      setFormData({
        socialAccountId: '',
        keywords: '',
        frequencyHours: 6,
        status: 'ACTIVE',
        promptTemplate: '',
      });
      loadData();
    } catch (error: any) {
      console.error('Error creating config:', error);
      alert(error.response?.data?.message || 'Failed to create config');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this Auto Pilot config?')) {
      return;
    }

    try {
      await api.delete(`/api/autopilot/${id}`);
      alert('Config deleted successfully');
      loadData();
    } catch (error) {
      console.error('Error deleting config:', error);
      alert('Failed to delete config');
    }
  };

  const handleToggleStatus = async (config: AutoPilotConfig) => {
    try {
      const newStatus = config.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
      await api.patch(`/api/autopilot/${config.id}`, { status: newStatus });
      loadData();
    } catch (error) {
      console.error('Error updating status:', error);
      alert('Failed to update status');
    }
  };

  const handleTrigger = async (id: string) => {
    if (!confirm('Manually trigger this Auto Pilot now?')) {
      return;
    }

    try {
      await api.post(`/api/autopilot/${id}/trigger`);
      alert('Auto Pilot triggered successfully! Check your scheduled posts.');
      loadData();
    } catch (error) {
      console.error('Error triggering auto pilot:', error);
      alert('Failed to trigger auto pilot');
    }
  };

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return 'Never';
    return new Date(dateStr).toLocaleString();
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'text-green-600 bg-green-100';
      case 'PAUSED':
        return 'text-yellow-600 bg-yellow-100';
      case 'STOPPED':
        return 'text-red-600 bg-red-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-xl">Loading...</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">🤖 AI Auto Pilot</h1>
          <p className="text-gray-600 mt-2">
            Automatically generate and schedule posts using AI
          </p>
        </div>
        <Button onClick={() => setShowCreateForm(!showCreateForm)}>
          {showCreateForm ? 'Cancel' : '+ New Auto Pilot'}
        </Button>
      </div>

      {showCreateForm && (
        <Card className="mb-8 p-6">
          <h2 className="text-xl font-semibold mb-4">Create Auto Pilot Config</h2>
          <form onSubmit={handleCreate} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Social Account
              </label>
              <select
                value={formData.socialAccountId}
                onChange={(e) =>
                  setFormData({ ...formData, socialAccountId: e.target.value })
                }
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="">Select an account</option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.accountName} ({account.platform})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Keywords (comma-separated)
              </label>
              <Input
                value={formData.keywords}
                onChange={(e) =>
                  setFormData({ ...formData, keywords: e.target.value })
                }
                placeholder="technology, AI, innovation, startup"
                required
              />
              <p className="text-sm text-gray-500 mt-1">
                AI will randomly pick one keyword per post
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Frequency (hours)
              </label>
              <Input
                type="number"
                min="1"
                value={formData.frequencyHours}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    frequencyHours: parseInt(e.target.value),
                  })
                }
                required
              />
              <p className="text-sm text-gray-500 mt-1">
                How often to generate and schedule posts
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Prompt Template (optional)
              </label>
              <textarea
                value={formData.promptTemplate}
                onChange={(e) =>
                  setFormData({ ...formData, promptTemplate: e.target.value })
                }
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                rows={3}
                placeholder="Custom prompt template (leave empty for default)"
              />
            </div>

            <div className="flex gap-4">
              <Button type="submit">Create Auto Pilot</Button>
              <Button
                type="button"
                onClick={() => setShowCreateForm(false)}
                className="bg-gray-500 hover:bg-gray-600"
              >
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      )}

      {configs.length === 0 ? (
        <Card className="p-8 text-center">
          <div className="text-6xl mb-4">🤖</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            No Auto Pilot Configs Yet
          </h3>
          <p className="text-gray-600 mb-4">
            Create your first Auto Pilot config to start automating your social media posts
          </p>
          <Button onClick={() => setShowCreateForm(true)}>
            Create First Auto Pilot
          </Button>
        </Card>
      ) : (
        <div className="grid gap-6">
          {configs.map((config) => (
            <Card key={config.id} className="p-6">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-xl font-semibold">
                      {config.socialAccountName}
                    </h3>
                    <span
                      className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(
                        config.status
                      )}`}
                    >
                      {config.status}
                    </span>
                  </div>
                  <p className="text-gray-600">Platform: {config.platform}</p>
                </div>
                <div className="flex gap-2">
                  <Button
                    onClick={() => handleToggleStatus(config)}
                    className={
                      config.status === 'ACTIVE'
                        ? 'bg-yellow-500 hover:bg-yellow-600'
                        : 'bg-green-500 hover:bg-green-600'
                    }
                  >
                    {config.status === 'ACTIVE' ? 'Pause' : 'Activate'}
                  </Button>
                  <Button
                    onClick={() => handleTrigger(config.id)}
                    className="bg-purple-500 hover:bg-purple-600"
                  >
                    Trigger Now
                  </Button>
                  <Button
                    onClick={() => handleDelete(config.id)}
                    className="bg-red-500 hover:bg-red-600"
                  >
                    Delete
                  </Button>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <p className="text-sm text-gray-600">Keywords:</p>
                  <div className="flex flex-wrap gap-2 mt-1">
                    {config.keywords.map((keyword, idx) => (
                      <span
                        key={idx}
                        className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-sm"
                      >
                        {keyword}
                      </span>
                    ))}
                  </div>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Frequency:</p>
                  <p className="font-medium">Every {config.frequencyHours} hours</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-600">Last Run:</p>
                  <p className="font-medium">{formatDate(config.lastRunAt)}</p>
                </div>
                <div>
                  <p className="text-gray-600">Next Run:</p>
                  <p className="font-medium">{formatDate(config.nextRunAt)}</p>
                </div>
              </div>

              {config.promptTemplate && (
                <div className="mt-4 pt-4 border-t">
                  <p className="text-sm text-gray-600 mb-1">Custom Prompt Template:</p>
                  <p className="text-sm bg-gray-50 p-2 rounded">
                    {config.promptTemplate}
                  </p>
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
