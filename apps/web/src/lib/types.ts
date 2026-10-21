// Espelho dos contratos da API (apps/api). Mantenha em sincronia com os DTOs Java.

export type MissionType = "MICRO" | "STANDARD" | "DEEP";
export type SessionKind = "LESSON" | "REINFORCEMENT" | "REVIEW";
export type SessionStatus = "IN_PROGRESS" | "PAUSED" | "COMPLETED";
export type TopicState = "NOT_STARTED" | "LEARNING" | "NEEDS_REINFORCEMENT" | "IN_REVIEW" | "MASTERED";
export type StudyStrategy =
  | "EDITAL_ORDER"
  | "UNSTUDIED_FIRST"
  | "LOW_PERFORMANCE"
  | "BY_SUBJECT"
  | "REVIEW_FIRST"
  | "BOARD_FOCUS";
export type StepType = "OBJECTIVE" | "EXPLANATION" | "EXAMPLE" | "QUESTION" | "SUMMARY";
export type Coverage = "NONE" | "QUESTIONS_ONLY" | "LESSON_ONLY" | "COVERED";

export interface Preferences {
  strategy: StudyStrategy;
  strategySubjectId: string | null;
  preferredMissionType: MissionType;
  focusMode: boolean;
  reduceMotion: boolean;
  showTimer: boolean;
  streaksEnabled: boolean;
  notificationsEnabled: boolean;
}

export interface Me {
  id: string;
  email: string;
  displayName: string;
  selectedRoleId: string | null;
  preferences: Preferences;
}

export interface Exam {
  id: string;
  name: string;
  board: string;
  year: number;
  description: string | null;
  sourceRef: string | null;
}

export interface Role {
  id: string;
  examId: string;
  name: string;
  description: string | null;
}

export interface Topic {
  id: string;
  subjectId: string;
  parentId: string | null;
  depth: number;
  editalCode: string | null;
  name: string;
  description: string | null;
  objectives: string[];
  lessons: { id: string; title: string }[];
  lessonCount: number;
  questionCount: number;
  coverage: Coverage;
}

export interface Subject {
  id: string;
  area: "GERAIS" | "ESPECIFICOS";
  name: string;
  description: string | null;
  examQuestionCount: number | null;
  topics: Topic[];
}

export interface Syllabus {
  exam: Exam;
  role: Role;
  syllabusVersionId: string;
  version: string;
  sourceRef: string | null;
  reviewedAt: string | null;
  subjects: Subject[];
}

export interface Mission {
  kind: SessionKind;
  missionType: MissionType;
  topicId: string;
  topicName: string;
  subjectName: string;
  lessonId: string | null;
  lessonTitle: string | null;
  reviewId: string | null;
  estimatedMinutes: number;
  reason: string;
}

export interface InterruptedSession {
  sessionId: string;
  kind: SessionKind;
  topicId: string;
  lessonId: string | null;
  reviewId: string | null;
  topicName: string;
  lessonTitle: string | null;
  currentStep: number;
  totalSteps: number;
  answered: number;
  totalQuestions: number;
  lastActivityAt: string;
}

export interface PendingReview {
  reviewId: string;
  topicId: string;
  topicName: string;
  dueAt: string;
  due: boolean;
}

export interface Journey {
  needsOnboarding: boolean;
  displayName: string;
  exam: Exam | null;
  role: Role | null;
  recommended: Mission | null;
  shortAlternative: Mission | null;
  interrupted: InterruptedSession | null;
  progress: {
    coveragePercent: number;
    topicsStarted: number;
    topicsMastered: number;
    topicsTotal: number;
    attempts: number;
    accuracy: number;
    xp: number;
  } | null;
  dueReviews: number;
  upcomingReviews: PendingReview[];
}

export interface PublicOption {
  label: string;
  text: string;
}

export interface PublicQuestion {
  id: string;
  topicId: string;
  context: string | null;
  statement: string;
  difficulty: "FACIL" | "MEDIA" | "DIFICIL";
  source: string;
  options: PublicOption[];
}

export interface FeedbackOption {
  label: string;
  text: string;
  rationale: string;
  correct: boolean;
}

export interface Feedback {
  questionId: string;
  selectedOption: string;
  correctOption: string;
  correct: boolean;
  explanation: string;
  options: FeedbackOption[];
}

export interface Step {
  index: number;
  type: StepType;
  question: PublicQuestion | null;
  feedback: Feedback | null;
}

export interface LessonContent {
  id: string;
  title: string;
  objective: string;
  explanation: string;
  example: string;
  summary: string | null;
  learningObjectives: string[];
}

export interface SessionResult {
  correct: number;
  total: number;
  score: number;
  xpAwarded: number;
  topicState: TopicState;
  masteryMilestone: boolean;
  nextReviewAt: string | null;
}

export interface Session {
  id: string;
  kind: SessionKind;
  missionType: MissionType;
  status: SessionStatus;
  currentStep: number;
  startedAt: string;
  lastActivityAt: string;
  topic: { id: string; name: string; subjectId: string; subjectName: string };
  lesson: LessonContent | null;
  reviewId: string | null;
  steps: Step[];
  answeredCount: number;
  totalQuestions: number;
  result: SessionResult | null;
}

export interface AnswerResponse {
  feedback: Feedback;
  alreadyAnswered: boolean;
  xpEarned: number;
  topicState: TopicState;
  currentStep: number;
}

export interface TopicProgress {
  topicId: string;
  subjectId: string;
  name: string;
  state: TopicState;
  attempts: number;
  correct: number;
  accuracy: number;
  lastStudiedAt: string | null;
  masteryReachedAt: string | null;
}

export interface SubjectProgress {
  subjectId: string;
  name: string;
  area: string;
  topicsTotal: number;
  topicsStarted: number;
  topicsMastered: number;
  attempts: number;
  correct: number;
  accuracy: number;
}

export interface ProgressSummary {
  xp: number;
  edital: { topicsTotal: number; topicsStarted: number; topicsMastered: number; coveragePercent: number };
  questions: { attempts: number; correct: number; accuracy: number };
  reviews: { due: number; scheduled: number };
  subjects: SubjectProgress[];
  topics: TopicProgress[];
}

export interface StartSessionRequest {
  kind: SessionKind;
  missionType?: MissionType;
  lessonId?: string | null;
  topicId?: string | null;
  reviewId?: string | null;
}
