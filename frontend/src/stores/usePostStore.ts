import { create } from "zustand";
import axiosInstance from "@/libs/axios";

const toLocalDatetimeInputValue = (date: Date) => {
  const pad = (value: number) => String(value).padStart(2, '0')
  return [date.getFullYear(), pad(date.getMonth() + 1), pad(date.getDate())].join('-') + 'T' + [pad(date.getHours()), pad(date.getMinutes())].join(':')
}

type Platform = 'FACEBOOK' | 'INSTAGRAM' | 'THREADS' | 'TIKTOK'

type SocialAccount = {
  id: string
  platform: Platform
  accountName?: string | null
  accountAlias?: string | null
  autoPilot: boolean
}

type ApiResponse<T> = { success: boolean; message: string; data: T }

type AiSourceOption = { id: string; contentPreview: string; mediaCount: number; createdAt: string }

type ScheduledPostPreview = { id: string; content: string; mediaUrl: string; scheduledTime: string; status: string; socialAccountName: string }

type MonitorSummary = { pending: number; processing: number; posted: number; failed: number }

type MonitorItem = { id: string; status: string; retryCount: number; scheduledTime: string; lastAttemptAt: string; publishedPostId: string; publishedPostUrl?: string | null; errorMessage: string }

type PostHistoryItem = any

type PagedHistoryResponse = { items: PostHistoryItem[]; total: number; page: number; size: number; hasNext: boolean }

interface PostStoreState {
  // data
  accounts: SocialAccount[]
  summary: MonitorSummary | null
  recent: MonitorItem[]
  aiGenerationOptions: AiSourceOption[]
  imageGenerationOptions: AiSourceOption[]
  history: PostHistoryItem[]

  // ui + form
  composeMode: 'MANUAL' | 'AI'
  aiGenerationLogId: string
  imageGenerationId: string
  selectedAccountId: string
  selectedHistoryId: string
  content: string
  mediaUrl: string
  uploadedMediaUrls: string[]
  scheduledTime: string
  preview: ScheduledPostPreview | null

  // history meta
  historyStatus: string
  historyPlatform: string
  historySearch: string
  historyPage: number
  historyPageSize: number
  historyTotal: number
  historyHasNext: boolean

  // flags
  loading: boolean
  submitting: boolean
  historyLoading: boolean
  actioningPostId: string
  message: string
  error: string

  // actions
  loadData: () => Promise<void>
  loadHistory: () => Promise<void>
  loadAiSources: () => Promise<void>
  uploadMediaFiles: (files: File[]) => Promise<string[]>
  clearUploadedMedia: () => void
  previewPost: (opts?: { ai?: boolean; overrideScheduledTime?: string }) => Promise<void>
  schedulePost: (opts?: { ai?: boolean; overrideScheduledTime?: string }) => Promise<void>
  postNow: () => Promise<void>
  cancelPost: (postId: string) => Promise<void>
  rescheduleInOneHour: (postId: string) => Promise<void>

  // setters
  setComposeMode: (mode: 'MANUAL' | 'AI') => void
  setAiGenerationLogId: (id: string) => void
  setImageGenerationId: (id: string) => void
  setSelectedAccountId: (id: string) => void
  setContent: (value: string) => void
  setMediaUrl: (value: string) => void
  setUploadedMediaUrls: (list: string[]) => void
  setScheduledTime: (value: string) => void
  setHistoryFilters: (filters: { status?: string; platform?: string; search?: string }) => void
  setHistoryPage: (p: number | ((prev: number) => number)) => void
  setSelectedHistoryId: (id: string) => void
}

export const usePostStore = create<PostStoreState>((set, get) => ({
  accounts: [],
  summary: null,
  recent: [],
  aiGenerationOptions: [],
  imageGenerationOptions: [],
  history: [],

  composeMode: 'MANUAL',
  aiGenerationLogId: '',
  imageGenerationId: '',
  selectedAccountId: '',
  selectedHistoryId: '',
  content: '',
  mediaUrl: '',
  uploadedMediaUrls: [],
  scheduledTime: toLocalDatetimeInputValue(new Date(Date.now() + 10 * 60 * 1000)),
  preview: null,

  historyStatus: '',
  historyPlatform: '',
  historySearch: '',
  historyPage: 0,
  historyPageSize: 6,
  historyTotal: 0,
  historyHasNext: false,

  loading: false,
  submitting: false,
  historyLoading: false,
  actioningPostId: '',
  message: '',
  error: '',

  loadData: async () => {
    set({ loading: true, error: '' });
    try {
      const [accountsResponse, summaryResponse, recentResponse, aiSourcesResponse] = await Promise.all([
        axiosInstance.get<ApiResponse<SocialAccount[]>>('/social-accounts'),
        axiosInstance.get<ApiResponse<MonitorSummary>>('/posts/monitor/summary'),
        axiosInstance.get<ApiResponse<MonitorItem[]>>('/posts/monitor/recent'),
        axiosInstance.get<ApiResponse<{ aiGenerationLogs: AiSourceOption[]; imageGenerations: AiSourceOption[] }>>('/posts/ai/sources?limit=20').catch(() => null),
      ])

      const loadedAccounts = accountsResponse.data.data ?? []
      set({ accounts: loadedAccounts, summary: summaryResponse.data.data ?? null, recent: recentResponse.data.data ?? [] })
      set({ aiGenerationOptions: aiSourcesResponse?.data?.data?.aiGenerationLogs ?? [], imageGenerationOptions: aiSourcesResponse?.data?.data?.imageGenerations ?? [] })

      if (!get().selectedAccountId && loadedAccounts.length > 0) {
        set({ selectedAccountId: loadedAccounts[0].id })
      }
    } catch (err: any) {
      set({ error: err?.response?.data?.message || 'Cannot load posting data right now.' })
    } finally {
      set({ loading: false })
    }
  },

  loadHistory: async () => {
    set({ historyLoading: true, error: '' })
    try {
      const params = new URLSearchParams()
      params.set('page', String(get().historyPage))
      params.set('size', String(get().historyPageSize))
      if (get().historyStatus) params.set('status', get().historyStatus)
      if (get().historyPlatform) params.set('platform', get().historyPlatform)
      if (get().historySearch) params.set('search', get().historySearch)

      const response = await axiosInstance.get<ApiResponse<PagedHistoryResponse>>(`/posts/history?${params.toString()}`)
      const payload = response.data.data
      set({ history: payload?.items ?? [], historyTotal: payload?.total ?? 0, historyHasNext: payload?.hasNext ?? false })
    } catch (err: any) {
      set({ error: err?.response?.data?.message || 'Cannot load post history right now.' })
    } finally {
      set({ historyLoading: false })
    }
  },

  loadAiSources: async () => {
    try {
      const response = await axiosInstance.get<ApiResponse<{ aiGenerationLogs: AiSourceOption[]; imageGenerations: AiSourceOption[] }>>('/posts/ai/sources?limit=20')
      set({ aiGenerationOptions: response.data.data?.aiGenerationLogs ?? [], imageGenerationOptions: response.data.data?.imageGenerations ?? [] })
    } catch (err: any) {
      set({ error: err?.response?.data?.message || 'Cannot load AI sources right now.' })
    }
  },

  uploadMediaFiles: async (files: File[]) => {
    set({ submitting: true, error: '', message: '' })
    try {
      console.debug('[usePostStore] uploadMediaFiles: files', files.map(f => f.name))
      const uploadedUrls: string[] = []
      for (const file of files) {
        const formData = new FormData()
        formData.append('file', file)
        const response = await axiosInstance.post<ApiResponse<{ secureUrl: string }>>('/media/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
        const uploadedUrl = response.data.data?.secureUrl
        if (uploadedUrl) uploadedUrls.push(uploadedUrl)
      }
      if (uploadedUrls.length === 0) throw new Error('No uploaded URL returned')
      const current = get().uploadedMediaUrls
      const merged = Array.from(new Set([...current, ...uploadedUrls]))
      set({ uploadedMediaUrls: merged })
      if (!get().mediaUrl) set({ mediaUrl: uploadedUrls[0] })
      set({ message: `Uploaded ${uploadedUrls.length} file(s) to Cloudinary.` })
      return uploadedUrls
    } catch (err: any) {
      set({ error: err?.response?.data?.message || 'Cannot upload media right now.' })
      return []
    } finally {
      set({ submitting: false })
    }
  },

  clearUploadedMedia: () => set({ uploadedMediaUrls: [], mediaUrl: '' }),

  previewPost: async (opts) => {
    set({ submitting: true, error: '', message: '' })
    try {
      if (!get().selectedAccountId) {
        set({ error: 'Select a social account first.' })
        return
      }
      if (!get().scheduledTime) {
        set({ error: 'Scheduled time is required.' })
        return
      }
      const { ai = false, overrideScheduledTime } = opts || {}
      if (ai) {
        const aiPayload = {
          socialAccountId: get().selectedAccountId,
          scheduledTime: overrideScheduledTime ?? get().scheduledTime,
          aiGenerationLogId: get().aiGenerationLogId || undefined,
          imageGenerationId: get().imageGenerationId || undefined,
          contentOverride: get().content.trim() ? get().content : undefined,
          mediaUrl: get().uploadedMediaUrls[0] ?? get().mediaUrl ?? undefined,
          mediaUrls: get().uploadedMediaUrls.length ? get().uploadedMediaUrls : undefined,
        }
        console.debug('[usePostStore] previewPost (AI) payload:', aiPayload)
        const response = await axiosInstance.post<ApiResponse<any>>('/posts/ai/preview', aiPayload)
        const aiData = response.data.data
        set({ preview: aiData?.post ?? null })
        if (!get().mediaUrl && aiData?.resolvedMediaUrls?.length) set({ mediaUrl: aiData.resolvedMediaUrls[0] })
        set({ message: `AI preview loaded (${String(aiData?.contentSource ?? 'unknown')}).` })
      } else {
        if (!get().content.trim()) {
          set({ error: 'Content is required.' })
          return
        }
        const payload = {
          socialAccountId: get().selectedAccountId,
          content: get().content,
          mediaUrl: get().uploadedMediaUrls[0] ?? get().mediaUrl ?? undefined,
          mediaUrls: get().uploadedMediaUrls.length ? get().uploadedMediaUrls : undefined,
          scheduledTime: get().scheduledTime,
        }
        console.debug('[usePostStore] previewPost payload:', payload)
        const response = await axiosInstance.post<ApiResponse<ScheduledPostPreview>>('/posts/preview', payload)
        set({ preview: response.data.data ?? null, message: 'Preview loaded.' })
      }
    } catch (err: any) {
      set({ error: err?.response?.data?.message || 'Cannot load preview right now.' })
    } finally {
      set({ submitting: false })
    }
  },

  schedulePost: async (opts) => {
    set({ submitting: true, error: '', message: '' })
    try {
      if (!get().selectedAccountId) {
        set({ error: 'Select a social account first.' })
        return
      }
      if (!get().scheduledTime) {
        set({ error: 'Scheduled time is required.' })
        return
      }
      const { ai = false, overrideScheduledTime } = opts || {}
      if (ai) {
        const payload = {
          socialAccountId: get().selectedAccountId,
          scheduledTime: overrideScheduledTime ?? get().scheduledTime,
          aiGenerationLogId: get().aiGenerationLogId || undefined,
          imageGenerationId: get().imageGenerationId || undefined,
          contentOverride: get().content.trim() ? get().content : undefined,
          mediaUrl: get().uploadedMediaUrls[0] ?? get().mediaUrl ?? undefined,
          mediaUrls: get().uploadedMediaUrls.length ? get().uploadedMediaUrls : undefined,
        }
        console.debug('[usePostStore] schedulePost (AI) payload:', payload)
        const response = await axiosInstance.post<ApiResponse<any>>('/posts/ai/schedule', payload)
        const aiData = response.data.data
        set({ preview: aiData?.post ?? null, message: `AI post scheduled (${String(aiData?.contentSource ?? 'unknown')}).` })
      } else {
        if (!get().content.trim()) {
          set({ error: 'Content is required.' })
          return
        }
        const payload = {
          socialAccountId: get().selectedAccountId,
          content: get().content,
          mediaUrl: get().uploadedMediaUrls[0] ?? get().mediaUrl ?? undefined,
          mediaUrls: get().uploadedMediaUrls.length ? get().uploadedMediaUrls : undefined,
          scheduledTime: get().scheduledTime,
        }
        console.debug('[usePostStore] schedulePost payload:', payload)
        const response = await axiosInstance.post<ApiResponse<ScheduledPostPreview>>('/posts/schedule', payload)
        set({ preview: response.data.data ?? null, message: 'Post scheduled successfully.' })
      }
      await get().loadData()
    } catch (err: any) {
      console.error('[usePostStore] schedulePost error:', err)
      console.error('[usePostStore] schedulePost error response data:', err?.response?.data)
      set({ error: err?.response?.data?.message || String(err?.message) || 'Cannot schedule post right now.' })
    } finally {
      set({ submitting: false })
    }
  },

  postNow: async () => {
    set({ submitting: true, error: '', message: '' })
    try {
      if (!get().selectedAccountId) {
        set({ error: 'Select a social account first.' })
        return
      }
      const immediateScheduledTime = new Date(Date.now() + 60 * 1000)
      const localIso = toLocalDatetimeInputValue(immediateScheduledTime)

      if (get().composeMode === 'AI') {
        const payload = {
          socialAccountId: get().selectedAccountId,
          scheduledTime: localIso,
          aiGenerationLogId: get().aiGenerationLogId || undefined,
          imageGenerationId: get().imageGenerationId || undefined,
          contentOverride: get().content.trim() ? get().content : undefined,
          mediaUrl: get().uploadedMediaUrls[0] ?? get().mediaUrl ?? undefined,
          mediaUrls: get().uploadedMediaUrls.length ? get().uploadedMediaUrls : undefined,
        }
        console.debug('[usePostStore] postNow (AI) payload:', payload)
        const response = await axiosInstance.post<ApiResponse<any>>('/posts/ai/schedule', payload)
        const payloadResp = response.data.data
        set({ preview: payloadResp?.post ?? null, message: `Post sent to queue for immediate publish (${String(payloadResp?.contentSource ?? 'unknown')}).` })
      } else {
        const payload = {
          socialAccountId: get().selectedAccountId,
          content: get().content,
          mediaUrl: get().uploadedMediaUrls[0] ?? get().mediaUrl ?? undefined,
          mediaUrls: get().uploadedMediaUrls.length ? get().uploadedMediaUrls : undefined,
          scheduledTime: localIso,
        }
        console.debug('[usePostStore] postNow payload:', payload)
        const response = await axiosInstance.post<ApiResponse<ScheduledPostPreview>>('/posts/schedule', payload)
        set({ preview: response.data.data ?? null, message: 'Post sent to queue for immediate publish.' })
      }

      set({ scheduledTime: toLocalDatetimeInputValue(new Date(Date.now() + 10 * 60 * 1000)) })
      await get().loadData()
    } catch (err: any) {
      set({ error: err?.response?.data?.message || 'Cannot post right now.' })
    } finally {
      set({ submitting: false })
    }
  },

  cancelPost: async (postId: string) => {
    set({ actioningPostId: postId, error: '', message: '' })
    try {
      await axiosInstance.patch<ApiResponse<ScheduledPostPreview>>(`/posts/${postId}/cancel`)
      set({ message: 'Post cancelled.' })
      await Promise.all([get().loadData(), get().loadHistory()])
    } catch (err: any) {
      set({ error: err?.response?.data?.message || 'Cannot cancel this post right now.' })
    } finally {
      set({ actioningPostId: '' })
    }
  },

  rescheduleInOneHour: async (postId: string) => {
    const scheduledAt = new Date(Date.now() + 60 * 60 * 1000)
    const pad = (value: number) => String(value).padStart(2, '0')
    const localIso = [scheduledAt.getFullYear(), pad(scheduledAt.getMonth() + 1), pad(scheduledAt.getDate())].join('-') + 'T' + [pad(scheduledAt.getHours()), pad(scheduledAt.getMinutes())].join(':')
    set({ actioningPostId: postId, error: '', message: '' })
    try {
      await axiosInstance.patch<ApiResponse<ScheduledPostPreview>>(`/posts/${postId}/reschedule`, { scheduledTime: localIso })
      set({ message: 'Post rescheduled for one hour from now.' })
      await Promise.all([get().loadData(), get().loadHistory()])
    } catch (err: any) {
      set({ error: err?.response?.data?.message || 'Cannot reschedule this post right now.' })
    } finally {
      set({ actioningPostId: '' })
    }
  },

  // setters
  setComposeMode: (mode) => set({ composeMode: mode }),
  setAiGenerationLogId: (id) => set({ aiGenerationLogId: id }),
  setImageGenerationId: (id) => set({ imageGenerationId: id }),
  setSelectedAccountId: (id) => set({ selectedAccountId: id }),
  setContent: (value) => set({ content: value }),
  setMediaUrl: (value) => set({ mediaUrl: value }),
  setUploadedMediaUrls: (list) => set({ uploadedMediaUrls: list }),
  setScheduledTime: (value) => set({ scheduledTime: value }),
  setHistoryFilters: (filters) => set({ historyStatus: filters.status ?? '', historyPlatform: filters.platform ?? '', historySearch: filters.search ?? '' }),
  setHistoryPage: (p) => {
    if (typeof p === 'function') {
      const current = get().historyPage
      const next = p(current)
      set({ historyPage: next })
    } else {
      set({ historyPage: p })
    }
  },
  setSelectedHistoryId: (id) => set({ selectedHistoryId: id }),
}));

export default usePostStore;
