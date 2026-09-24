import styles from './Button.module.css'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost'

export interface ButtonStyleProps {
  variant?: ButtonVariant
  small?: boolean
}

export function buttonClassName(
  { variant = 'primary', small = false }: ButtonStyleProps,
  className?: string,
): string {
  return [styles.button, styles[variant], small ? styles.small : '', className]
    .filter(Boolean)
    .join(' ')
}
