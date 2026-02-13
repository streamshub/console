/**
 * Hook for tracking recently viewed topics
 * 
 * Uses localStorage to persist the last 5 viewed topics per cluster.
 * This provides a "Recent Topics" feature in the Overview page.
 */

import { useCallback, useMemo } from 'react';

export interface ViewedTopic {
  kafkaId: string;
  topicId: string;
  topicName: string;
  timestamp: number;
}

const STORAGE_KEY = 'streamshub-viewed-topics';
const MAX_VIEWED_TOPICS = 5;

/**
 * Get all viewed topics from localStorage
 */
function getViewedTopicsFromStorage(): ViewedTopic[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return [];
    
    const parsed = JSON.parse(stored);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error('Failed to parse viewed topics from localStorage:', error);
    return [];
  }
}

/**
 * Save viewed topics to localStorage
 */
function saveViewedTopicsToStorage(topics: ViewedTopic[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(topics));
  } catch (error) {
    console.error('Failed to save viewed topics to localStorage:', error);
  }
}

/**
 * Hook to manage viewed topics for a specific Kafka cluster
 */
export function useViewedTopics(kafkaId: string | undefined) {
  // Get viewed topics for this cluster
  const viewedTopics = useMemo(() => {
    if (!kafkaId) return [];
    
    const allTopics = getViewedTopicsFromStorage();
    return allTopics
      .filter((topic) => topic.kafkaId === kafkaId)
      .sort((a, b) => b.timestamp - a.timestamp) // Most recent first
      .slice(0, MAX_VIEWED_TOPICS);
  }, [kafkaId]);

  // Add a topic to the viewed list
  const addViewedTopic = useCallback(
    (topicId: string, topicName: string) => {
      if (!kafkaId) return;

      const allTopics = getViewedTopicsFromStorage();
      
      // Remove existing entry for this topic (if any)
      const filteredTopics = allTopics.filter(
        (topic) => !(topic.kafkaId === kafkaId && topic.topicId === topicId)
      );

      // Add new entry at the beginning
      const newTopic: ViewedTopic = {
        kafkaId,
        topicId,
        topicName,
        timestamp: Date.now(),
      };

      const updatedTopics = [newTopic, ...filteredTopics];

      // Keep only the most recent entries per cluster (limit total storage)
      const limitedTopics = updatedTopics.slice(0, MAX_VIEWED_TOPICS * 10);

      saveViewedTopicsToStorage(limitedTopics);
    },
    [kafkaId]
  );

  // Clear all viewed topics for this cluster
  const clearViewedTopics = useCallback(() => {
    if (!kafkaId) return;

    const allTopics = getViewedTopicsFromStorage();
    const filteredTopics = allTopics.filter((topic) => topic.kafkaId !== kafkaId);
    
    saveViewedTopicsToStorage(filteredTopics);
  }, [kafkaId]);

  return {
    viewedTopics,
    addViewedTopic,
    clearViewedTopics,
  };
}